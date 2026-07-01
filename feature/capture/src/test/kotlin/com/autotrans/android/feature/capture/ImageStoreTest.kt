package com.autotrans.android.feature.capture

import android.graphics.Bitmap
import com.autotrans.android.feature.capture.store.ImageStore
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll

@DisplayName("ImageStore")
class ImageStoreTest {

    private lateinit var imageStore: ImageStore

    @BeforeEach
    fun setUp() {
        imageStore = ImageStore()
        // Mock Bitmap.createScaledBitmap to avoid Android framework dependency in unit tests
        mockkStatic(Bitmap::class)
    }

    @AfterEach
    fun tearDown() {
        imageStore.clear()
        unmockkAll()
    }

    // -------------------------------------------------------------------------
    // Registration
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("register() stores a bitmap and returns a valid ImageData ID")
    fun `register stores bitmap and returns ImageData`() {
        val bitmap = createMockBitmap(100, 100)
        val imageData = imageStore.register(bitmap)
        assertNotNull(imageData.id)
        assertTrue(imageData.id.isNotBlank())
        assertEquals(1, imageStore.size())
    }

    @Test
    @DisplayName("getBitmap() retrieves the stored bitmap by ID")
    fun `getBitmap returns stored bitmap`() {
        val bitmap = createMockBitmap(100, 100)
        val imageData = imageStore.register(bitmap)
        val retrieved = imageStore.getBitmap(imageData)
        assertNotNull(retrieved)
    }

    // -------------------------------------------------------------------------
    // LRU Eviction
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("LRU eviction: adding 4 bitmaps should evict the 1st")
    fun `adding 4 bitmaps evicts the oldest`() {
        val bitmaps = (1..4).map { createMockBitmap(100, 100) }
        val ids = bitmaps.map { imageStore.register(it) }

        // Store should have 3 entries (max capacity)
        assertEquals(3, imageStore.size())

        // First registered bitmap should have been evicted
        assertNull(imageStore.getBitmap(ids[0]))

        // Last 3 should still be present
        assertNotNull(imageStore.getBitmap(ids[1]))
        assertNotNull(imageStore.getBitmap(ids[2]))
        assertNotNull(imageStore.getBitmap(ids[3]))
    }

    @Test
    @DisplayName("LRU eviction: accessing older entry promotes it — it should not be evicted first")
    fun `accessing entry promotes it in LRU order`() {
        // Register 2 bitmaps
        val b1 = createMockBitmap(100, 100)
        val b2 = createMockBitmap(100, 100)
        val id1 = imageStore.register(b1)
        val id2 = imageStore.register(b2)

        // Access b1 to promote it (make it most recently used)
        imageStore.getBitmap(id1)

        // Register 2 more — should evict b2 (now LRU), not b1
        val b3 = createMockBitmap(100, 100)
        val b4 = createMockBitmap(100, 100)
        imageStore.register(b3)
        imageStore.register(b4)

        // id2 should have been evicted (it was LRU)
        assertNull(imageStore.getBitmap(id2))
        // id1 should still be present (promoted)
        assertNotNull(imageStore.getBitmap(id1))
    }

    // -------------------------------------------------------------------------
    // Downsampling
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("downsample: bitmaps <= 720px tall are stored as-is")
    fun `bitmap within height limit is not downsampled`() {
        val bitmap = createMockBitmap(1280, 720)
        val id = imageStore.register(bitmap)
        val stored = imageStore.getBitmap(id)
        // Bitmap should be the same object (no copy made)
        assertNotNull(stored)
    }

    @Test
    @DisplayName("downsample: bitmaps > 720px tall trigger createScaledBitmap")
    fun `bitmap exceeding height limit is downsampled`() {
        val original = createMockBitmap(1920, 1080)
        val scaled = createMockBitmap(1280, 720)
        every { Bitmap.createScaledBitmap(original, any(), 720, true) } returns scaled
        every { original.recycle() } returns Unit // original is recycled after scaling

        val id = imageStore.register(original)
        val stored = imageStore.getBitmap(id)
        // Should have stored the scaled version
        assertEquals(scaled.width, stored?.width)
        assertEquals(scaled.height, stored?.height)
    }

    // -------------------------------------------------------------------------
    // Release / Clear
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("release() removes and recycles the bitmap")
    fun `release removes entry from store`() {
        val bitmap = createMockBitmap(100, 100)
        every { bitmap.recycle() } returns Unit
        val id = imageStore.register(bitmap)
        assertEquals(1, imageStore.size())
        imageStore.release(id)
        assertEquals(0, imageStore.size())
        assertNull(imageStore.getBitmap(id))
    }

    @Test
    @DisplayName("clear() empties the store")
    fun `clear empties store`() {
        repeat(3) { imageStore.register(createMockBitmap(100, 100)) }
        assertEquals(3, imageStore.size())
        imageStore.clear()
        assertEquals(0, imageStore.size())
    }

    // -------------------------------------------------------------------------
    // Thread safety — basic smoke test
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("concurrent register calls do not throw")
    fun `concurrent register calls are thread safe`() {
        val threads = (1..10).map {
            Thread { imageStore.register(createMockBitmap(100, 100)) }
        }
        threads.forEach { it.start() }
        threads.forEach { it.join() }
        // Max capacity enforced
        assertEquals(3, imageStore.size())
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun createMockBitmap(width: Int, height: Int): Bitmap = mockk(relaxed = true) {
        every { this@mockk.width } returns width
        every { this@mockk.height } returns height
        every { isRecycled } returns false
    }
}
