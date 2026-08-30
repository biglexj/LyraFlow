package com.biglexj.lyraflow.platform

import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SingleInstanceLockTest {

    @BeforeTest
    @AfterTest
    fun cleanup() {
        SingleInstanceLock.release()
        System.clearProperty("lyraflow.channel")
        System.clearProperty("lyraflow.dev")
        System.clearProperty("lyraflow.allowMultipleInstances")
        System.clearProperty("lyraflow.lock.port")
    }

    @Test
    fun channelResolutionDefaultsToStable() {
        assertEquals("stable", SingleInstanceLock.getChannel())
        assertEquals(SingleInstanceLock.STABLE_PORT, SingleInstanceLock.getPortForChannel())
        assertFalse(SingleInstanceLock.isDevMode())
    }

    @Test
    fun channelResolutionIdentifiesDev() {
        System.setProperty("lyraflow.channel", "dev")
        assertEquals("dev", SingleInstanceLock.getChannel())
        assertEquals(SingleInstanceLock.DEV_PORT, SingleInstanceLock.getPortForChannel())
        assertTrue(SingleInstanceLock.isDevMode())
    }

    @Test
    fun channelResolutionIdentifiesTest() {
        System.setProperty("lyraflow.channel", "test")
        assertEquals("test", SingleInstanceLock.getChannel())
        assertEquals(49285, SingleInstanceLock.getPortForChannel())
    }

    @Test
    fun allowMultipleInstancesBypassesLock() {
        System.setProperty("lyraflow.allowMultipleInstances", "true")
        assertTrue(SingleInstanceLock.allowsMultipleInstances())
        assertTrue(SingleInstanceLock.acquireOrTransfer())
    }

    @Test
    fun primaryAcquiresAndSecondaryTransfersPayload() {
        System.setProperty("lyraflow.channel", "test")
        val latch = CountDownLatch(1)
        var receivedPayload: String? = null

        val primaryAcquired = SingleInstanceLock.acquireOrTransfer { payload ->
            receivedPayload = payload
            latch.countDown()
        }
        assertTrue(primaryAcquired, "Primary instance should acquire the lock")

        // Intento de segunda instancia: no debe adquirir el lock y debe transferir el comando
        val secondaryAcquired = SingleInstanceLock.acquireOrTransfer(args = arrayOf("--test-arg"))
        assertFalse(secondaryAcquired, "Secondary instance should fail to acquire lock")

        // Verificar que el payload llegó a la instancia primaria
        val received = latch.await(3, TimeUnit.SECONDS)
        assertTrue(received, "Primary instance should receive the payload from secondary instance")
        assertEquals("--test-arg", receivedPayload)

        // Liberar y verificar que el socket queda libre
        SingleInstanceLock.release()
        Thread.sleep(100)

        // Comprobar que tras release se puede volver a adquirir
        val reacquired = SingleInstanceLock.acquireOrTransfer()
        assertTrue(reacquired, "Lock should be re-acquirable after release")
    }
}
