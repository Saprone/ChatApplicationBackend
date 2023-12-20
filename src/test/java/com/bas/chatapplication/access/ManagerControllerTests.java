package com.bas.chatapplication.access;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ManagerControllerTests {
    @MockBean
    ManagerController managerController = new ManagerController();

    @Test
    void testVerifyToken() {
        ResponseEntity<String> responseEntity = ResponseEntity.status(HttpStatus.OK).body("Verification successful.");

        Assertions.assertNotNull(responseEntity.getBody());
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        Assertions.assertEquals("Verification successful.", responseEntity.getBody());
    }

    @Test
    void testGet() {
        Assertions.assertNotNull(managerController.get());
        Assertions.assertEquals("GET request successful from manager controller.", (managerController.get()));
    }

    @Test
    void testPost() {
        Assertions.assertNotNull(managerController.post());
        Assertions.assertEquals("POST request successful from manager controller.", (managerController.post()));
    }

    @Test
    void testPut() {
        Assertions.assertNotNull(managerController.put());
        Assertions.assertEquals("PUT request successful from manager controller.", (managerController.put()));
    }

    @Test
    void testDelete() {
        Assertions.assertNotNull(managerController.delete());
        Assertions.assertEquals("DELETE request successful from manager controller.", (managerController.delete()));
    }
}
