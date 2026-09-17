package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentExtractorInternalTest {

    @Test
    fun `readTextOperators handles hex strings`() {
        val content = "<48 65 6c 6c 6f> Tj"
        val result = DocumentExtractor.readTextOperators(content)
        assertEquals("Hello", result.trim())
    }

    @Test
    fun `readTextOperators handles hex strings with odd digits`() {
        val content = "<48656c6c6> Tj" // 9 digits, should be padded to <48656c6c60>
        val result = DocumentExtractor.readTextOperators(content)
        assertEquals("Hello", result.trim())
    }

    @Test
    fun `readTextOperators handles quote operators`() {
        val content = "(Hello) ' (World) \""
        val result = DocumentExtractor.readTextOperators(content)
        assertEquals("Hello\nWorld\n", result)
    }

    @Test
    fun `readTextOperators handles TJ arrays`() {
        val content = "[(H) 10 (e) 10 (l) 10 (l) 10 (o)] TJ"
        val result = DocumentExtractor.readTextOperators(content)
        assertEquals("Hello", result.trim())
    }

    @Test
    fun `readTextOperators handles mixed literal and hex in TJ`() {
        val content = "[(H) 10 <45> 10 (L) 10 <4c> 10 (O)] TJ"
        val result = DocumentExtractor.readTextOperators(content)
        assertEquals("HELLO", result.trim())
    }

    @Test
    fun `readTextOperators handles positioning operators as newlines`() {
        val content = "(Line 1) Tj 70 700 Td (Line 2) Tj"
        val result = DocumentExtractor.readTextOperators(content)
        assertTrue(result.contains("Line 1"))
        assertTrue(result.contains("Line 2"))
        assertTrue(result.contains("\n"))
    }
    
    @Test
    fun `readTextOperators handles UTF-16BE hex strings`() {
        // FEFF is the BOM for UTF-16BE
        // 0048 0069 is "Hi"
        val content = "<FEFF00480069> Tj"
        val result = DocumentExtractor.readTextOperators(content)
        assertEquals("Hi", result.trim())
    }
}
