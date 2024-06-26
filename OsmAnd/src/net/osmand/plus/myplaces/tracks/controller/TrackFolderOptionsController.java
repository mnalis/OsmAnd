package net.osmand.plus.myplaces.tracks.controller;

import static net.osmand.plus.base.dialog.data.DialogExtra.BACKGROUND_COLOR;
import static net.osmand.plus.utils.FileUtils.ILLEGAL_PATH_NAME_CHARACTERS;

import android.content.Context;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.base.dialog.BaseDialogController;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.base.dialog.data.DisplayData;
import net.osmand.plus.base.dialog.data.DisplayItem;
import net.osmand.plus.base.dialog.interfaces.controller.IDialogItemClicked;
import net.osmand.plus.base.dialog.interfaces.controller.IDisplayDataProvider;
import net.osmand.plus.myplaces.tracks.TrackFoldersHelper;
import net.osmand.plus.settings.bottomsheets.CustomizableOptionsBottomSheet;
import net.osmand.plus.track.data.TrackFolder;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.alert.AlertDialogData;
import net.osmand.plus.widgets.alert.AlertDialogExtra;
import net.osmand.plus.widgets.alert.CustomAlert;
import net.osmand.util.Algorithms;

import java.io.File;

public class TrackFolderOptionsController extends BaseDialogController implements IDisplayDataProvider,
		IDialogItemClicked, TrackFolderOptionsListener {

	public static final String PROCESS_ID = "tracks_folder_options";

	private TrackFoldersHelper foldersHelper;
	private TrackFolder trackFolder;
	private TrackFolder parentFolder;
	private TrackFolderOptionsListener optionsListener;

	public TrackFolderOptionsController(@NonNull TrackFoldersHelper foldersHelper, @NonNull TrackFolder folder) {
		super(foldersHelper.getApp());
		this.trackFolder = folder;
		this.parentFolder = folder.getParentFolder();
		this.foldersHelper = foldersHelper;
	}

	public void setTrackFolderOptionsListener(@Nullable TrackFolderOptionsListener listener) {
		this.optionsListener = listener;
	}

	@NonNull
	@Override
	public String getProcessId() {
		return PROCESS_ID;
	}

	@Nullable
	@Override
	public DisplayData getDisplayData(@NonNull String processId) {
		UiUtilities iconsCache = app.getUIUtilities();
		DisplayData displayData = new DisplayData();
		int folderColorAlpha = ColorUtilities.getColorWithAlpha(trackFolder.getColor(), 0.3f);
		displayData.putExtra(BACKGROUND_COLOR, folderColorAlpha);

		displayData.addDisplayItem(new DisplayItem()
				.setTitle(trackFolder.getName(app))
				.setDescription(GpxUiHelper.getFolderDescription(app, trackFolder))
				.setLayoutId(R.layout.bottom_sheet_item_with_descr_72dp)
				.setIcon(iconsCache.getPaintedIcon(R.drawable.ic_action_folder, trackFolder.getColor()))
				.setShowBottomDivider(true, 0)
		);

		int dividerPadding = calculateSubtitleDividerPadding();
		for (TrackFolderOption listOption : TrackFolderOption.getAvailableOptions()) {
			displayData.addDisplayItem(new DisplayItem()
					.setTitle(getString(listOption.getTitleId()))
					.setLayoutId(R.layout.bottom_sheet_item_simple_56dp_padding_32dp)
					.setIcon(iconsCache.getThemedIcon(listOption.getIconId()))
					.setShowBottomDivider(listOption.shouldShowBottomDivider(), dividerPadding)
					.setTag(listOption)
			);
		}
		return displayData;
	}

	@Override
	public void onDialogItemClicked(@NonNull String processId, @NonNull DisplayItem item) {
		if (!(item.getTag() instanceof TrackFolderOption)) {
			return;
		}
		TrackFolderOption option = (TrackFolderOption) item.getTag();
		if (option == TrackFolderOption.DETAILS) {
			showDetails();
		} else if (option == TrackFolderOption.SHOW_ALL_TRACKS) {
			showFolderTracksOnMap(trackFolder);
		} else if (option == TrackFolderOption.EDIT_NAME) {
			showRenameDialog();
		} else if (option == TrackFolderOption.CHANGE_APPEARANCE) {
			showChangeAppearanceDialog(trackFolder);
		} else if (option == TrackFolderOption.EXPORT) {
			showExportDialog(trackFolder);
		} else if (option == TrackFolderOption.MOVE) {
			showMoveDialog();
		} else if (option == TrackFolderOption.DELETE_FOLDER) {
			showDeleteDialog();
		}
	}

	private void showDetails() {

	}

	@Override
	public void showFolderTracksOnMap(@NonNull TrackFolder folder) {
		dialogManager.askDismissDialog(PROCESS_ID);

		if (optionsListener != null) {
			optionsListener.showFolderTracksOnMap(folder);
		}
	}

	private void showRenameDialog() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			AlertDialogData dialogData = new AlertDialogData(activity, isNightMode())
					.setTitle(R.string.shared_string_rename)
					.setControlsColor(trackFolder.getColor())
					.setNegativeButton(R.string.shared_string_cancel, null);
			dialogData.setPositiveButton(R.string.shared_string_apply, (dialog, which) -> {
				Object extra = dialogData.getExtra(AlertDialogExtra.EDIT_TEXT);
				if (extra instanceof EditText) {
					EditText editText = (EditText) extra;
					String newName = editText.getText().toString();

					if (Algorithms.isBlank(newName)) {
						app.showToastMessage(R.string.empty_filename);
					} else if (ILLEGAL_PATH_NAME_CHARACTERS.matcher(newName).find()) {
						app.showToastMessage(R.string.file_name_containes_illegal_char);
					} else {
						File destFolder = new File(trackFolder.getDirFile().getParentFile(), newName);
						if (destFolder.exists()) {
							app.showToastMessage(R.string.file_with_name_already_exist);
						} else {
							renameFolder(newName);
						}
					}
				}
			});
			String caption = activity.getString(R.string.enter_new_name);
			CustomAlert.showInput(dialogData, activity, trackFolder.getDirName(), caption);
		}
	}

	private void renameFolder(@NonNull String newName) {
		foldersHelper.renameFolder(trackFolder, newName, folder -> {
			if (parentFolder != null) {
				parentFolder.removeSubFolder(trackFolder, false);
				parentFolder.addSubFolder(folder, true);
			}
			trackFolder = folder;
			dialogManager.askRefreshDialogCompletely(PROCESS_ID);

			// Notify external listener
			if (optionsListener != null) {
				optionsListener.onFolderRenamed(folder.getDirFile());
			}
			return true;
		});
	}

	@Override
	public void showChangeAppearanceDialog(@NonNull TrackFolder folder) {
		dialogManager.askDismissDialog(PROCESS_ID);

		if (optionsListener != null) {
			optionsListener.showChangeAppearanceDialog(folder);
		}
	}

	@Override
	public void showExportDialog(@NonNull TrackFolder folder) {
		dialogManager.askDismissDialog(PROCESS_ID);

		if (optionsListener != null) {
			optionsListener.showExportDialog(folder);
		}
	}

	private void showMoveDialog() {

	}

	private void showDeleteDialog() {
		Context ctx = getContext();
		if (ctx != null) {
			AlertDialogData dialogData = new AlertDialogData(ctx, isNightMode())
					.setTitle(R.string.delete_folder_question)
					.setNegativeButton(R.string.shared_string_cancel, null)
					.setPositiveButton(R.string.shared_string_delete, (dialog, which) -> {
						foldersHelper.deleteTrackFolder(trackFolder);
						onFolderDeleted();
					});
			String folderName = trackFolder.getName(ctx);
			String tracksCount = String.valueOf(trackFolder.getTotalTracksCount());
			String message = ctx.getString(R.string.delete_track_folder_dialog_message, folderName, tracksCount);
			CustomAlert.showSimpleMessage(dialogData, message);
		}
	}

	@Override
	public void onFolderDeleted() {
		// Close options dialog after folder deleted
		dialogManager.askDismissDialog(PROCESS_ID);

		// Notify external listener
		if (optionsListener != null) {
			optionsListener.onFolderDeleted();
		}
	}

	private int calculateSubtitleDividerPadding() {
		int contentPadding = getDimension(R.dimen.content_padding);
		int iconWidth = getDimension(R.dimen.standard_icon_size);
		return contentPadding * 3 + iconWidth;
	}

	public static void showDialog(@NonNull TrackFoldersHelper foldersHelper, @NonNull TrackFolder folder,
	                              @Nullable TrackFolderOptionsListener listener) {
		TrackFolderOptionsController controller = new TrackFolderOptionsController(foldersHelper, folder);
		controller.setTrackFolderOptionsListener(listener);

		DialogManager dialogManager = foldersHelper.getApp().getDialogManager();
		dialogManager.register(PROCESS_ID, controller);

		FragmentManager fragmentManager = foldersHelper.getActivity().getSupportFragmentManager();
		CustomizableOptionsBottomSheet.showInstance(fragmentManager, PROCESS_ID, false);
	}
}
