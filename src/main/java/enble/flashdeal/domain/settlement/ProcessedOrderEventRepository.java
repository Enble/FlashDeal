package enble.flashdeal.domain.settlement;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedOrderEventRepository extends JpaRepository<ProcessedOrderEvent, Long> {

    boolean existsByOrderIdAndConsumerGroup(Long orderId, String consumerGroup);
}
