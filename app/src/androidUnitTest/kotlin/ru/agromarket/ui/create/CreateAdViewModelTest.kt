package ru.agromarket.ui.create

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import ru.agromarket.data.draft.AdDraftManager
import ru.agromarket.data.model.AdDetailResponse
import ru.agromarket.data.model.AdPhotoResponse
import ru.agromarket.data.repository.AgroRepository
import ru.agromarket.data.repository.ApiResult

/**
 * Covers the `needs_revision` edit/resubmit flow: prefilling the wizard from an existing ad
 * and resubmitting via `updateAd` + `submitAd` instead of re-creating the ad.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CreateAdViewModelTest {

    private val repository: AgroRepository = mockk()
    private val draftManager: AdDraftManager = mockk(relaxed = true)

    private val adDetail = AdDetailResponse(
        id = "ad-1",
        userId = "user-1",
        type = "sale",
        categoryId = 10,
        categoryName = "Тракторы",
        parentCategoryName = "Техника",
        regionId = 5,
        regionName = "Краснодарский край",
        title = "Трактор МТЗ",
        description = "Описание",
        price = 500000.0,
        phonePrimary = "+79001234567",
        status = "needs_revision",
        boostLevel = "none",
        moderationComment = "Добавьте фото VIN",
        photos = listOf(
            AdPhotoResponse(id = 1, url = "https://example.com/p1.jpg", sortOrder = 0),
            AdPhotoResponse(id = 2, url = "https://example.com/p2.jpg", sortOrder = 1),
        ),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        coEvery { repository.getCategories() } returns ApiResult.Success(emptyList())
        coEvery { repository.getRegions() } returns ApiResult.Success(emptyList())
        coEvery { repository.getAdDetail("ad-1") } returns ApiResult.Success(adDetail)
        coEvery { repository.getDistricts(5) } returns ApiResult.Success(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadAdForEdit prefills wizard fields and jumps to the form step`() = runTest {
        val viewModel = CreateAdViewModel(repository, draftManager)

        viewModel.loadAdForEdit("ad-1")
        advanceUntilIdle()

        assertTrue(viewModel.isEditMode)
        assertEquals("Трактор МТЗ", viewModel.title)
        assertEquals("500000", viewModel.price)
        assertEquals("+79001234567", viewModel.phonePrimary)
        assertEquals(10, viewModel.selectedCategoryId)
        assertEquals(5, viewModel.selectedRegionId)
        assertEquals(2, viewModel.existingPhotos.size)
        assertEquals(CreateStep.FORM, viewModel.step)
    }

    @Test
    fun `submitAd in edit mode resubmits via updateAd then submitAd without re-creating the ad`() = runTest {
        coEvery { repository.updateAd("ad-1", any()) } returns ApiResult.Success(adDetail)
        coEvery { repository.submitAd("ad-1") } returns ApiResult.Success(adDetail)

        val viewModel = CreateAdViewModel(repository, draftManager)
        viewModel.loadAdForEdit("ad-1")
        advanceUntilIdle()

        var succeeded = false
        viewModel.submitAd(mockk(relaxed = true)) { succeeded = true }
        advanceUntilIdle()
        // submitAd hops to Dispatchers.IO to convert (empty) photoUris and back to Main;
        // give that real dispatch a moment to land before re-checking the test scheduler.
        Thread.sleep(100)
        advanceUntilIdle()

        assertTrue(succeeded)
        assertNull(viewModel.error)
        coVerify(exactly = 1) { repository.updateAd("ad-1", any()) }
        coVerify(exactly = 1) { repository.submitAd("ad-1") }
        coVerify(exactly = 0) { repository.createAd(any()) }
        coVerify(exactly = 0) { repository.uploadPhotos(any(), any()) }
    }

    @Test
    fun `submitAd requires at least MIN_PHOTOS combining existing and new photos`() = runTest {
        val viewModel = CreateAdViewModel(repository, draftManager)
        viewModel.loadAdForEdit("ad-1")
        advanceUntilIdle()

        // Simulate the user removing both existing photos, leaving nothing to submit.
        viewModel.existingPhotos = emptyList()

        viewModel.submitAd(mockk(relaxed = true)) { fail("onSuccess must not be called") }

        assertEquals("Загрузите минимум ${CreateAdViewModel.MIN_PHOTOS} фото", viewModel.error)
        coVerify(exactly = 0) { repository.updateAd(any(), any()) }
    }
}
