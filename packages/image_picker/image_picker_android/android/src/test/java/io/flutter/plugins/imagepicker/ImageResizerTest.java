// Copyright 2013 The Flutter Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.imagepicker;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

// RobolectricTestRunner always creates a default mock bitmap when reading from file. So we cannot actually test the scaling.
// But we can still test whether the original or scaled file is created.
@RunWith(RobolectricTestRunner.class)
public class ImageResizerTest {

  ImageResizer resizer;
  File imageFile;
  File externalDirectory;
  Bitmap originalImageBitmap;

  @Before
  public void setUp() throws IOException {
    MockitoAnnotations.initMocks(this);
    imageFile = new File(getClass().getClassLoader().getResource("pngImage.png").getFile());
    originalImageBitmap = BitmapFactory.decodeFile(imageFile.getPath());
    TemporaryFolder temporaryFolder = new TemporaryFolder();
    temporaryFolder.create();
    externalDirectory = temporaryFolder.newFolder("image_picker_testing_path");
    resizer = new ImageResizer(externalDirectory, new ExifDataCopier());
  }

  @Test
  public void onResizeImageIfNeeded_WhenQualityIsNull_ShoultNotResize_ReturnTheUnscaledFile() {
    String outoutFile = resizer.resizeImageIfNeeded(imageFile.getPath(), null, null, null);
    assertThat(outoutFile, equalTo(imageFile.getPath()));
  }

  @Test
  public void onResizeImageIfNeeded_WhenQualityIsNotNull_ShoulResize_ReturnResizedFile() {
    String outoutFile = resizer.resizeImageIfNeeded(imageFile.getPath(), null, null, 50);
    assertThat(outoutFile, equalTo(externalDirectory.getPath() + "/scaled_pngImage.png"));
  }

  @Test
  public void onResizeImageIfNeeded_WhenWidthIsNotNull_ShoulResize_ReturnResizedFile() {
    String outoutFile = resizer.resizeImageIfNeeded(imageFile.getPath(), 50.0, null, null);
    assertThat(outoutFile, equalTo(externalDirectory.getPath() + "/scaled_pngImage.png"));
  }

  @Test
  public void onResizeImageIfNeeded_WhenHeightIsNotNull_ShoulResize_ReturnResizedFile() {
    String outoutFile = resizer.resizeImageIfNeeded(imageFile.getPath(), null, 50.0, null);
    assertThat(outoutFile, equalTo(externalDirectory.getPath() + "/scaled_pngImage.png"));
  }

  @Test
  public void onResizeImageIfNeeded_WhenParentDirectoryDoesNotExists_ShouldNotCrash() {
    File nonExistentDirectory = new File(externalDirectory, "/nonExistent");
    ImageResizer invalidResizer = new ImageResizer(nonExistentDirectory, new ExifDataCopier());
    String outoutFile = invalidResizer.resizeImageIfNeeded(imageFile.getPath(), null, 50.0, null);
    assertThat(outoutFile, equalTo(nonExistentDirectory.getPath() + "/scaled_pngImage.png"));
  }

  @Test
  public void onResizeImageIfNeeded_WhenResizeIsNotNecessary_ShouldOnlyQueryBitmap() {
    try (MockedStatic<BitmapFactory> mockBitmapFactory =
        mockStatic(BitmapFactory.class, Mockito.CALLS_REAL_METHODS)) {
      String outoutFile = resizer.resizeImageIfNeeded(imageFile.getPath(), null, null, null);
      ArgumentCaptor<BitmapFactory.Options> argument =
          ArgumentCaptor.forClass(BitmapFactory.Options.class);
      mockBitmapFactory.verify(() -> BitmapFactory.decodeFile(anyString(), argument.capture()));
      BitmapFactory.Options capturedOptions = argument.getValue();
      assertTrue(capturedOptions.inJustDecodeBounds);
    }
  }

  @Test
  public void onResizeImageIfNeeded_WhenResizeIsNecessary_ShouldAllocateBitmap() {
    try (MockedStatic<BitmapFactory> mockBitmapFactory =
        mockStatic(BitmapFactory.class, Mockito.CALLS_REAL_METHODS)) {
      String outoutFile = resizer.resizeImageIfNeeded(imageFile.getPath(), 50.0, 50.0, null);
      ArgumentCaptor<BitmapFactory.Options> argument =
          ArgumentCaptor.forClass(BitmapFactory.Options.class);
      mockBitmapFactory.verify(
          () -> BitmapFactory.decodeFile(anyString(), argument.capture()), times(2));
      List<BitmapFactory.Options> capturedOptions = argument.getAllValues();
      assertTrue(capturedOptions.get(0).inJustDecodeBounds);
      assertFalse(capturedOptions.get(1).inJustDecodeBounds);
    }
  }
}
