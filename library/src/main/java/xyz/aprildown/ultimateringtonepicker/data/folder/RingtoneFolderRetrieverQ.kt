package xyz.aprildown.ultimateringtonepicker.data.folder

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import xyz.aprildown.ultimateringtonepicker.UltimateRingtonePicker
import xyz.aprildown.ultimateringtonepicker.data.Category
import xyz.aprildown.ultimateringtonepicker.data.Ringtone

@RequiresApi(Build.VERSION_CODES.Q)
internal class RingtoneFolderRetrieverQ(private val context: Context) : RingtoneFolderRetriever {

    private data class MutableFolder(
        val folderId: Long,
        val folderName: String,
        var count: Int = 0
    )

    override fun getRingtoneFolders(): List<Category> {
        val folders = mutableListOf<MutableFolder>()
        // This is hack. Is there any better way?
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            arrayOf(
                MediaStore.Audio.Media.BUCKET_ID,
                MediaStore.Audio.Media.BUCKET_DISPLAY_NAME
            ),
            null,
            null,
            MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME
        )?.use {
            it.moveToPosition(-1)
            while (it.moveToNext()) {
                try {
                    val bucketId =
                        it.getLong(it.getColumnIndexOrThrow(MediaStore.Audio.Media.BUCKET_ID))
                    val currentFolder = folders.find { folder -> folder.folderId == bucketId }
                    if (currentFolder == null) {
                        val bucketName = try {
                            it.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.BUCKET_DISPLAY_NAME))
                        } catch (e: Exception) {
                            // The bucketName may be null or doesn't exist.
                            continue
                        }
                        folders.add(MutableFolder(bucketId, bucketName, count = 1))
                    } else {
                        currentFolder.count += 1
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return folders.map {
            Category(
                type = UltimateRingtonePicker.RingtoneCategoryType.Folder,
                id = it.folderId,
                name = it.folderName,
                numberOfSongs = it.count
            )
        }
    }

    override fun getRingtonesFromFolder(folderId: Long): List<Ringtone> {
        val data = mutableListOf<Ringtone>()
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE
            ),
            "${MediaStore.Audio.Media.BUCKET_ID} = $folderId",
            null,
            MediaStore.Audio.Media.TITLE_KEY
        )?.use {
            it.moveToPosition(-1)
            while (it.moveToNext()) {
                try {
                    val uri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        it.getLong(it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                    )
                    val title = it.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE))
                    data.add(Ringtone(uri, title))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return data
    }
}
