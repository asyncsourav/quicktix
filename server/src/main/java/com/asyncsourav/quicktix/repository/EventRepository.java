

package com.asyncsourav.quicktix.repository;


import com.asyncsourav.quicktix.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;



public interface EventRepository extends JpaRepository<Event, Long> {


    List<Event> findByOrganizerId(Long organizerId);
    List<Event> findByCategoryIgnoreCase(String category);
    List<Event> findByVenueId(Long venueId);
    List<Event> findByStartTimeAfterOrderByStartTimeAsc(LocalDateTime now);
    List<Event> findByTitleContainingIgnoreCase(String titleKeyword);
}
