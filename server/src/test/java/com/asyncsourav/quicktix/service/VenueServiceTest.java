

package com.asyncsourav.quicktix.service;




import com.asyncsourav.quicktix.dto.request.VenueCreateRequest;
import com.asyncsourav.quicktix.dto.response.VenueResponse;
import com.asyncsourav.quicktix.entity.Role;
import com.asyncsourav.quicktix.entity.User;
import com.asyncsourav.quicktix.entity.Venue;
import com.asyncsourav.quicktix.exception.ResourceNotFoundException;
import com.asyncsourav.quicktix.repository.UserRepository;
import com.asyncsourav.quicktix.repository.VenueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;




@ExtendWith(MockitoExtension.class)
class VenueServiceTest {

    @Mock
    private VenueRepository venueRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private VenueService venueService;

    private User organizer;
    private Venue venue;

 
    @BeforeEach
    void setUp() {
   
        organizer = User.builder()
                .id(5L)
                .name("Venue Owner")
                .email("owner@quicktix.com")
                .passwordHash("hash")
                .role(Role.ORGANIZER)
                .build();

        venue = Venue.builder()
                .id(15L)
                .name("Grand Hall")
                .address("Main Street")
                .totalCapacity(50)
                .organizer(organizer)
                .build();
    }



    @Test
    @DisplayName("createVenue: Persists a venue for the authenticated organizer")
    void createVenue_ShouldPersistAndReturnVenue() {

        VenueCreateRequest request = new VenueCreateRequest("Grand Hall", "Main Street", 50);
        when(userRepository.findByEmail("owner@quicktix.com")).thenReturn(Optional.of(organizer));
        when(venueRepository.save(any(Venue.class))).thenAnswer(invocation -> {
            Venue saved = invocation.getArgument(0);
            saved.setId(15L);
            return saved;
        });

        VenueResponse response = venueService.createVenue(request, "owner@quicktix.com");

        assertThat(response.id()).isEqualTo(15L);
        assertThat(response.name()).isEqualTo("Grand Hall");
        assertThat(response.organizerId()).isEqualTo(5L);

        ArgumentCaptor<Venue> venueCaptor = ArgumentCaptor.forClass(Venue.class);
        verify(venueRepository).save(venueCaptor.capture());
        assertThat(venueCaptor.getValue().getTotalCapacity()).isEqualTo(50);
    }




    @Test
    @DisplayName("createVenue: Throws when organizer user record is missing")
    void createVenue_WhenUserMissing_ShouldThrowResourceNotFound() {

        VenueCreateRequest request = new VenueCreateRequest("Grand Hall", "Main Street", 50);
        when(userRepository.findByEmail("missing@quicktix.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> venueService.createVenue(request, "missing@quicktix.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with email");
    }




    @Test
    @DisplayName("getAllVenues: Returns all venues mapped to responses")
    void getAllVenues_ShouldReturnMappedList() {

        when(venueRepository.findAll()).thenReturn(List.of(venue));
        List<VenueResponse> venues = venueService.getAllVenues();

        assertThat(venues).hasSize(1);
        assertThat(venues.get(0).name()).isEqualTo("Grand Hall");
    }




    @Test
    @DisplayName("getVenueById: Returns venue when found")
    void getVenueById_ShouldReturnVenue() {

        when(venueRepository.findById(15L)).thenReturn(Optional.of(venue));

        VenueResponse response = venueService.getVenueById(15L);

        assertThat(response.id()).isEqualTo(15L);
        assertThat(response.organizerName()).isEqualTo("Venue Owner");
    }




    @Test
    @DisplayName("getVenuesByOrganizer: Returns venues belonging to organizer")
    void getVenuesByOrganizer_ShouldReturnOrganizerVenues() {

        when(venueRepository.findByOrganizerId(5L)).thenReturn(List.of(venue));
        List<VenueResponse> venues = venueService.getVenuesByOrganizer(5L);

        assertThat(venues).hasSize(1);
        assertThat(venues.get(0).organizerId()).isEqualTo(5L);
    }




    @Test
    @DisplayName("updateVenue: Allows owner to update venue")
    void updateVenue_ShouldAllowOwnerToUpdateVenue() {

        VenueCreateRequest request = new VenueCreateRequest("Updated Hall", "New Address", 80);
        when(venueRepository.findById(15L)).thenReturn(Optional.of(venue));
        when(userRepository.findByEmail("owner@quicktix.com")).thenReturn(Optional.of(organizer));
        when(venueRepository.save(any(Venue.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VenueResponse response = venueService.updateVenue(15L, request, "owner@quicktix.com");

        assertThat(response.name()).isEqualTo("Updated Hall");
        assertThat(response.totalCapacity()).isEqualTo(80);
    }



    @Test
    @DisplayName("updateVenue: Rejects unauthorized user")
    void updateVenue_WhenUserIsNotOwnerOrAdmin_ShouldThrowAccessDenied() {

        User otherUser = User.builder().id(7L).name("Other User").email("other@quicktix.com").passwordHash("hash").role(Role.USER).build();
        VenueCreateRequest request = new VenueCreateRequest("Updated Hall", "New Address", 80);

        when(venueRepository.findById(15L)).thenReturn(Optional.of(venue));
        when(userRepository.findByEmail("other@quicktix.com")).thenReturn(Optional.of(otherUser));

        assertThatThrownBy(() -> venueService.updateVenue(15L, request, "other@quicktix.com"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("permission to modify this venue");
    }




    @Test
    @DisplayName("deleteVenue: Allows admin to delete venue")
    void deleteVenue_ShouldAllowAdminToDeleteVenue() {
        
        User admin = User.builder().id(99L).name("Admin User").email("admin@quicktix.com").passwordHash("hash").role(Role.ADMIN).build();
        when(venueRepository.findById(15L)).thenReturn(Optional.of(venue));
        when(userRepository.findByEmail("admin@quicktix.com")).thenReturn(Optional.of(admin));

        venueService.deleteVenue(15L, "admin@quicktix.com");

        verify(venueRepository).delete(venue);
    }
}
