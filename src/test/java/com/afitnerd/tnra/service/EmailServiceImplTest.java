package com.afitnerd.tnra.service;

import com.afitnerd.tnra.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private PostRenderer postRenderer;

    @Mock
    private UserRepository userRepository;

    private EmailServiceImpl emailService;
    private Method splitMethod;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        emailService = new EmailServiceImpl(postRenderer, userRepository);
        splitMethod = EmailServiceImpl.class.getDeclaredMethod("split", String.class, String.class);
        splitMethod.setAccessible(true);
    }

    @Test
    void splitReturnsSingleCharacterChunksWhenPrefixConsumesMaxLength() throws Exception {
        ReflectionTestUtils.setField(emailService, "maxEmailToSmsLength", 8);

        @SuppressWarnings("unchecked")
        List<String> chunks = (List<String>) splitMethod.invoke(emailService, "12345678", "abcd");

        assertEquals(List.of("a", "b", "c", "d"), chunks);
    }

    @Test
    void splitUsesRemainingCharacterBudgetPerChunk() throws Exception {
        ReflectionTestUtils.setField(emailService, "maxEmailToSmsLength", 15);

        @SuppressWarnings("unchecked")
        List<String> chunks = (List<String>) splitMethod.invoke(emailService, "prefix", "abcdefghij");

        assertEquals(2, chunks.size());
        assertEquals("abcdefghi", chunks.get(0));
        assertEquals("j", chunks.get(1));
    }

    @Test
    void splitReturnsEmptyListForEmptyOrNullInput() throws Exception {
        ReflectionTestUtils.setField(emailService, "maxEmailToSmsLength", 20);

        @SuppressWarnings("unchecked")
        List<String> empty = (List<String>) splitMethod.invoke(emailService, "prefix", "");
        @SuppressWarnings("unchecked")
        List<String> nullInput = (List<String>) splitMethod.invoke(emailService, "prefix", null);

        assertNotNull(empty);
        assertNotNull(nullInput);
        assertTrue(empty.isEmpty());
        assertTrue(nullInput.isEmpty());
    }
}
