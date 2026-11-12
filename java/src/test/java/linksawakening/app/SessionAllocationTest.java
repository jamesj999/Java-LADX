package linksawakening.app;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.management.ThreadMXBean;
import java.lang.management.ManagementFactory;
import linksawakening.testing.RequiresRom;
import linksawakening.testing.TestRom;
import org.junit.jupiter.api.Test;

@RequiresRom
final class SessionAllocationTest {
    @Test
    void simulationAllocation() throws Exception {
        try (var rom = TestRom.open()) {
            assertTrue(rom.read() >= 0);
        }
        var bean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        assertTrue(bean.isThreadAllocatedMemorySupported());
        bean.setThreadAllocatedMemoryEnabled(true);
        var session = new GameSession();
        session.initializeHeadlessNewGame();
        for (int i = 0; i < 600; i++) session.stepWithoutRender();
        long thread = Thread.currentThread().threadId();
        long before = bean.getThreadAllocatedBytes(thread);
        for (int i = 0; i < 600; i++) session.stepWithoutRender();
        long bytesPerFrame = (bean.getThreadAllocatedBytes(thread) - before) / 600;
        System.out.println("Simulation allocation bytes/frame: " + bytesPerFrame);
        assertTrue(bytesPerFrame >= 0);
    }
}
