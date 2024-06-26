package org.changppo.tracking.api;

import org.changppo.tracking.api.request.StartTrackingRequest;
import org.changppo.tracking.api.response.TrackingResponse;
import org.changppo.tracking.base.WithCustomMockUser;
import org.junit.jupiter.api.Nested;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.changppo.tracking.api.request.GenerateTokenRequest;
import org.changppo.tracking.api.request.TrackingRequest;
import org.changppo.tracking.api.response.GenerateTokenResponse;
import org.changppo.tracking.service.TrackingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.geo.Point;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
        import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = {TrackingController.class}, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
public class TrackingControllerTest {
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TrackingService trackingService;

    private final String baseUrl = "/api/tracking/v1";

    @Nested
    @DisplayName("<Tracking>")
    class mvpTest {
        @DisplayName("토큰을 성공적으로 발급함")
        @Test
        void successGenerateToken() throws Exception {
            // given
            GenerateTokenRequest request = new GenerateTokenRequest(
                    "DEFAULT",
                    List.of("READ_TRACKING_COORDINATE"),
                    3600L);
            GenerateTokenResponse response = new GenerateTokenResponse("TOKEN");
            given(trackingService.generateToken(anyString(), any(GenerateTokenRequest.class))).willReturn(response);

            //when
            mockMvc.perform(post(baseUrl + "/generate-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("api-key-id", "test")
                            .content(objectMapper.writeValueAsString(request)))
                    //then
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value(response.getToken()))
                    .andDo(print());
        }

        @DisplayName("Tracking 시작 성공")
        @WithCustomMockUser
        @Test
        void successStartTracking() throws Exception {
            // given
            StartTrackingRequest request = new StartTrackingRequest(
                    1,1,
                    2,2,
                    3L
            );

            //when
            mockMvc.perform(post(baseUrl + "/start")
                            .header("Authorization", "Bearer {TOKEN}")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    //then
                    .andExpect(status().isOk())
                    .andDo(print());
        }

        @DisplayName("Tracking 성공함")
        @WithCustomMockUser
        @Test
        void successTracking() throws Exception {
            // given
            TrackingRequest request = new TrackingRequest(new Point(1,2));

            //when
            mockMvc.perform(post(baseUrl + "/tracking")
                            .header("Authorization", "Bearer {TOKEN}")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    //then
                    .andExpect(status().isOk())
                    .andDo(print());
        }

        @DisplayName("Tracking 성공적으로 마침")
        @WithCustomMockUser
        @Test
        void successTrackingFinish() throws Exception {
            // given

            //when
            mockMvc.perform(delete(baseUrl + "/finish")
                            .header("Authorization", "Bearer {ACCESS_TOKEN}"))
                    //then
                    .andExpect(status().isOk())
                    .andDo(print());
        }

        @DisplayName("Tracking 정보 받아오기 성공")
        @WithCustomMockUser
        @Test
        void successGetTracking() throws Exception {
            // given
            TrackingResponse response = new TrackingResponse(1,1);
            given(trackingService.getTracking(any())).willReturn(response);

            //when
            mockMvc.perform(get(baseUrl + "/tracking")
                            .header("Authorization", "Bearer {ACCESS_TOKEN}"))
                    //then
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.locations.x").value(1))
                    .andExpect(jsonPath("$.locations.y").value(2))
                    .andDo(print());
        }
    }
}