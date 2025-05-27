package net.javaguides.compatablefuture.service;


import net.javaguides.compatablefuture.domain.User;
import net.javaguides.compatablefuture.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.boot.context.event.ApplicationReadyEvent;

import java.util.Random;
import java.util.concurrent.*;
import java.util.stream.IntStream;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final BlockingQueue<User> userQueue;
    private final int producerCount;
    private final int consumerCount;
    private final ExecutorService executorService;

    public UserService(UserRepository userRepository,
                       @Value("${producer.threads:2}") int producerCount,
                       @Value("${consumer.threads:2}") int consumerCount) {
        this.userRepository = userRepository;
        this.userQueue = new LinkedBlockingQueue<>();
        this.producerCount = producerCount;
        this.consumerCount = consumerCount;
        this.executorService = Executors.newFixedThreadPool(producerCount + consumerCount);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startProcessing() {
        logger.info("Starting {} producers and {} consumers...", producerCount, consumerCount);

        IntStream.rangeClosed(1, producerCount)
                .forEach(i -> executorService.submit(new UserProducer(userQueue, i)));

        IntStream.rangeClosed(1, consumerCount)
                .forEach(i -> executorService.submit(new UserConsumer(userQueue, userRepository, i)));
    }

//    @PreDestroy
//    public void shutdown() {
//        logger.info("Shutting down user service...");
//        executorService.shutdownNow();
//    }

    static class UserProducer implements Runnable {
        private final BlockingQueue<User> userQueue;
        private final int producerId;
        private final Random random = new Random();
        private int userCounter = 1;

        public UserProducer(BlockingQueue<User> userQueue, int producerId) {
            this.userQueue = userQueue;
            this.producerId = producerId;
        }

        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    String username = "user_" + producerId + "_" + userCounter++;
                    User user = new User(username);
                    userQueue.put(user);
                    logger.info("Producer {} created user: {}", producerId, username);
                    Thread.sleep(random.nextInt(500));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.info("Producer {} interrupted.", producerId);
            }
        }
    }

    static class UserConsumer implements Runnable {
        private final BlockingQueue<User> userQueue;
        private final UserRepository userRepository;
        private final int consumerId;

        public UserConsumer(BlockingQueue<User> userQueue, UserRepository userRepository, int consumerId) {
            this.userQueue = userQueue;
            this.userRepository = userRepository;
            this.consumerId = consumerId;
        }

        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    User user = userQueue.take();
                    userRepository.save(user);
                    logger.info("Consumer {} saved user: {}", consumerId, user.getUsername());
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.info("Consumer {} interrupted.", consumerId);
            }
        }
    }
}
