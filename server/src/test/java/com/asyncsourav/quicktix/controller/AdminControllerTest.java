

package com.asyncsourav.quicktix.controller;




import com.asyncsourav.quicktix.dto.response.AdminStatsResponse;
import com.asyncsourav.quicktix.dto.response.BookingResponse;
import com.asyncsourav.quicktix.dto.response.UserResponse;
import com.asyncsourav.quicktix.entity.BookingStatus;
import com.asyncsourav.quicktix.entity.Role;
import com.asyncsourav.quicktix.exception.GlobalExceptionHandler;
import com.asyncsourav.quicktix.service.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;




@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private AdminService adminService;

    @InjectMocks
    private AdminController adminController;

    private MockMvc mockMvc;



    @BeforeEach
    void setUp() {

        // Given: Set up MockMvc with exception handler
        mockMvc = MockMvcBuilders.standaloneSetup(adminController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }



    @Test
    @DisplayName("getStats: Returns 200 OK and aggregated platform metrics")
    
    void getStats_ShouldReturnPlatformMetrics() throws Exception {
        // Given
        AdminStatsResponse stats = new AdminStatsResponse(
                50L,
                10L,
                25L,
                120L,
                new BigDecimal("6400.00"),
                110L,
                10L
        );
        when(adminService.getPlatformStats()).thenReturn(stats);

        // When
        var resultActions = mockMvc.perform(get("/api/admin/stats"));

        // Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Platform statistics retrieved"))
                .andExpect(jsonPath("$.data.totalUsers").value(50L))
                .andExpect(jsonPath("$.data.totalVenues").value(10L))
                .andExpect(jsonPath("$.data.totalEvents").value(25L))
                .andExpect(jsonPath("$.data.totalBookings").value(120L))
                .andExpect(jsonPath("$.data.totalRevenue").value(6400.00))
                .andExpect(jsonPath("$.data.confirmedBookings").value(110L))
                .andExpect(jsonPath("$.data.cancelledBookings").value(10L));

        verify(adminService).getPlatformStats();
    }



    
    @Test
    @DisplayName("getAllUsers: Returns 200 OK and list of all platform users")
    void getAllUsers_ShouldReturnUserList() throws Exception {
    
        // Given
        UserResponse user1 = new UserResponse(1L, "Alice", "alice@quicktix.com", Role.USER, LocalDateTime.now());
        UserResponse user2 = new UserResponse(2L, "Bob", "bob@quicktix.com", Role.ORGANIZER, LocalDateTime.now());
        when(adminService.getAllUsers()).thenReturn(List.of(user1, user2));

        // When
        var resultActions = mockMvc.perform(get("/api/admin/users"));

        // Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("All users retrieved"))
                .andExpect(jsonPath("$.data[0].id").value(1L))
                .andExpect(jsonPath("$.data[0].email").value("alice@quicktix.com"))
                .andExpect(jsonPath("$.data[1].id").value(2L))
                .andExpect(jsonPath("$.data[1].email").value("bob@quicktix.com"));

        verify(adminService).getAllUsers();
    }



    
    @Test
    @DisplayName("getAllBookings: Returns 200 OK and list of all bookings across platform")
    void getAllBookings_ShouldReturnAllBookings() throws Exception {
    
        // Given
        BookingResponse booking = new BookingResponse(
                500L,
                1L,
                "Alice",
                10L,
                "Summer Concert",
                List.of(),
                new BigDecimal("80.00"),
                BookingStatus.CONFIRMED,
                LocalDateTime.now()
        );
        when(adminService.getAllBookings()).thenReturn(List.of(booking));

        // When
        var resultActions = mockMvc.perform(get("/api/admin/bookings"));

        // Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("All bookings retrieved"))
                .andExpect(jsonPath("$.data[0].id").value(500L))
                .andExpect(jsonPath("$.data[0].eventTitle").value("Summer Concert"))
                .andExpect(jsonPath("$.data[0].status").value("CONFIRMED"));

        verify(adminService).getAllBookings();
    }
}
