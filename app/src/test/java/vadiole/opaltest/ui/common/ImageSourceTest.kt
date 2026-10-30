package vadiole.opaltest.ui.common

import android.content.res.Resources
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import vadiole.opaltest.R

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ImageSourceTest {
    private val resources: Resources get() = RuntimeEnvironment.getApplication().resources

    private fun key(qualifiers: String): ImageKey {
        RuntimeEnvironment.setQualifiers(qualifiers)
        return (resources.imageSource(R.drawable.img_avatar_placeholder) as ImageSource.Bitmap).key
    }

    @Test
    @Config(qualifiers = "xxhdpi")
    fun keyFollowsResolvedVariant() {
        val xxhdpi = key("xxhdpi")
        val mdpi = key("mdpi")
        assertTrue(xxhdpi.path, xxhdpi.path.contains("xxhdpi"))
        assertTrue(mdpi.path, mdpi.path.contains("mdpi") && !mdpi.path.contains("xxhdpi"))
        assertNotEquals(xxhdpi, mdpi)
        assertEquals(key("xxhdpi"), xxhdpi)
    }

    @Test
    @Config(qualifiers = "xxhdpi")
    fun keyIncludesTargetDensity() {
        val xxhdpi = key("xxhdpi")
        val custom = key("440dpi")
        assertEquals(xxhdpi.path, custom.path)
        assertNotEquals(xxhdpi, custom)
    }

    @Test
    fun vectorDrawableFallsBackToPainter() {
        assertEquals(ImageSource.Vector(R.drawable.ic_gift), resources.imageSource(R.drawable.ic_gift))
    }
}
