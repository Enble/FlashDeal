package enble.flashdeal.domain.order;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"product"})
    @Query("SELECT o FROM Order o WHERE o.member.id = :memberId AND o.createdAt >= :windowStart ORDER BY o.createdAt DESC")
    List<Order> findRecentOrdersByMemberId(@Param("memberId") Long memberId,
                                           @Param("windowStart") LocalDateTime windowStart);
}
