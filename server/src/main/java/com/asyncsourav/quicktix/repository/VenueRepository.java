

package com.asyncsourav.quicktix.repository;


import com.asyncsourav.quicktix.entity.Venue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;



public interface VenueRepository extends JpaRepository<Venue, Long> {

    List<Venue> findByOrganizerId(Long organizerId);
    List<Venue> findByNameContainingIgnoreCase(String name);
}
