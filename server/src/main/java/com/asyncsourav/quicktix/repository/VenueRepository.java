

package com.asyncsourav.quicktix.repository;


import com.asyncsourav.quicktix.entity.Venue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;



@Repository
public interface VenueRepository extends JpaRepository<Venue, Long> {

    List<Venue> findByOrganizerId(Long organizerId);
    List<Venue> findByNameContainingIgnoreCase(String name);
}
