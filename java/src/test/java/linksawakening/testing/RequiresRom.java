package linksawakening.testing;

import java.lang.annotation.*;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;

/** Marks integration tests requiring the user's locally supplied ROM. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@Tag("rom")
@ExtendWith(RomCondition.class)
public @interface RequiresRom {}
