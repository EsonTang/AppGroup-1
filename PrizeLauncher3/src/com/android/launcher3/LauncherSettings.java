/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3;

import android.net.Uri;
import android.provider.BaseColumns;

import com.mediatek.launcher3.ext.AllApps;

/**
 * Settings related utilities.
 */
public class LauncherSettings {
    /** Columns required on table staht will be subject to backup and restore. */
    public static interface ChangeLogColumns extends BaseColumns {
        /**
         * The time of the last update to this row.
         * <P>Type: INTEGER</P>
         */
        static final String MODIFIED = "modified";
    }

    public static interface BaseLauncherColumns extends ChangeLogColumns {
        /**
         * Descriptive name of the gesture that can be displayed to the user.
         * <P>Type: TEXT</P>
         */
        static final String TITLE = "title";
        static final String TITLE_ID="title_id";//add by zhouerlong 

        /**
         * The Intent URL of the gesture, describing what it points to. This
         * value is given to {@link android.content.Intent#parseUri(String, int)} to create
         * an Intent that can be launched.
         * <P>Type: TEXT</P>
         */
        static final String INTENT = "intent";

        /**
         * The type of the gesture
         *
         * <P>Type: INTEGER</P>
         */
        static final String ITEM_TYPE = "itemType";

        /**
         * The gesture is an application
         */
        static final int ITEM_TYPE_APPLICATION = 0;

        /**
         * The gesture is an application created shortcut
         */
        static final int ITEM_TYPE_SHORTCUT = 1;

        /**
         * The icon type.
         * <P>Type: INTEGER</P>
         */
        static final String ICON_TYPE = "iconType";

        /**
         * The icon is a resource identified by a package name and an integer id.
         */
        static final int ICON_TYPE_RESOURCE = 0;

        /**
         * The icon is a bitmap.
         */
        static final int ICON_TYPE_BITMAP = 1;

        /**
         * The icon package name, if icon type is ICON_TYPE_RESOURCE.
         * <P>Type: TEXT</P>
         */
        static final String ICON_PACKAGE = "iconPackage";

        /**
         * The icon resource id, if icon type is ICON_TYPE_RESOURCE.
         * <P>Type: TEXT</P>
         */
        static final String ICON_RESOURCE = "iconResource";

        /**
         * The custom icon bitmap, if icon type is ICON_TYPE_BITMAP.
         * <P>Type: BLOB</P>
         */
        static final String ICON = "icon";
    }

    /**
     * Workspace Screens.
     *
     * Tracks the order of workspace screens.
     */
    public static final class WorkspaceScreens implements ChangeLogColumns {
        /**
         * The content:// style URL for this table
         */
        static final Uri CONTENT_URI = Uri.parse("content://" +
                AllApps.AUTHORITY + "/" + AllApps.TABLE_WORKSPACE_SCREENS +
                "?" + AllApps.PARAMETER_NOTIFY + "=true");

        /**
         * The rank of this screen -- ie. how it is ordered relative to the other screens.
         * <P>Type: INTEGER</P>
         */
        static final String SCREEN_RANK = "screenRank";
    }

    /**
     * Favorites.
     */
    public static final class Favorites implements BaseLauncherColumns {
        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" +
                AllApps.AUTHORITY + "/" + AllApps.TABLE_FAVORITES +
                "?" + AllApps.PARAMETER_NOTIFY + "=true");

        /**
         * The content:// style URL for this table
         */
        static final Uri OLD_CONTENT_URI = Uri.parse("content://" +
                AllApps.OLD_AUTHORITY + "/" + AllApps.TABLE_FAVORITES +
                "?" + AllApps.PARAMETER_NOTIFY + "=true");

        /**
         * The content:// style URL for this table. When this Uri is used, no notification is
         * sent if the content changes.
         */
        static final Uri CONTENT_URI_NO_NOTIFICATION = Uri.parse("content://" +
                AllApps.AUTHORITY + "/" + AllApps.TABLE_FAVORITES +
                "?" + AllApps.PARAMETER_NOTIFY + "=false");

        /**
         * The content:// style URL for a given row, identified by its id.
         *
         * @param id The row id.
         * @param notify True to send a notification is the content changes.
         *
         * @return The unique content URL for the specified row.
         */
        static Uri getContentUri(long id, boolean notify) {
            return Uri.parse("content://" + AllApps.AUTHORITY +
                    "/" + AllApps.TABLE_FAVORITES + "/" + id + "?" +
                    AllApps.PARAMETER_NOTIFY + "=" + notify);
        }

        /**
         * The container holding the favorite
         * <P>Type: INTEGER</P>
         */
        static final String CONTAINER = "container";

        /**
         * The icon is a resource identified by a package name and an integer id.
         */
        static final int CONTAINER_DESKTOP = -100;
        static final int CONTAINER_HOTSEAT = -101;
        static final int CONTAINER_HIDEVIEW = -102;

        /**
         * The screen holding the favorite (if container is CONTAINER_DESKTOP)
         * <P>Type: INTEGER</P>
         */
        static final String SCREEN = "screen";

        /**
         * The X coordinate of the cell holding the favorite
         * (if container is CONTAINER_HOTSEAT or CONTAINER_HOTSEAT)
         * <P>Type: INTEGER</P>
         */
        static final String CELLX = "cellX";

        /**
         * The Y coordinate of the cell holding the favorite
         * (if container is CONTAINER_DESKTOP)
         * <P>Type: INTEGER</P>
         */
        static final String CELLY = "cellY";

        /**
         * The X span of the cell holding the favorite
         * <P>Type: INTEGER</P>
         */
        static final String SPANX = "spanX";
        static final String TITLE_ID = "title_id";//add bu zhouerlong
        static final String FIRST_INSTALL = "first_install";
        static final String DOWN_PROGRESS = "down_progress";
        static final String PACKAGE_NAME = "package_name";
        static final String ICON_URI = "icon_uri";
        static final String FROM_APPSTORE = "from_appstore";
        static final String DOWN_STATE = "down_state";
    	 static final String HIDE="app_hide";

        /**
         * The Y span of the cell holding the favorite
         * <P>Type: INTEGER</P>
         */
        static final String SPANY = "spanY";

        /**
         * The favorite is a user created folder
         */
        static final int ITEM_TYPE_FOLDER = 2;

        /**
        * The favorite is a live folder
        *
        * Note: live folders can no longer be added to Launcher, and any live folders which
        * exist within the launcher database will be ignored when loading.  That said, these
        * entries in the database may still exist, and are not automatically stripped.
        */
        static final int ITEM_TYPE_LIVE_FOLDER = 3;

        /**
         * The favorite is a widget
         */
        static final int ITEM_TYPE_APPWIDGET = 4;

        /**
         * The favorite is a clock
         */
        static final int ITEM_TYPE_WIDGET_CLOCK = 1000;

        /**
         * The favorite is a search widget
         */
        static final int ITEM_TYPE_WIDGET_SEARCH = 1001;

        /**
         * The favorite is a photo frame
         */
        static final int ITEM_TYPE_WIDGET_PHOTO_FRAME = 1002;

        /**
         * The appWidgetId of the widget
         *
         * <P>Type: INTEGER</P>
         */
        static final String APPWIDGET_ID = "appWidgetId";

        /**
         * The ComponentName of the widget provider
         *
         * <P>Type: STRING</P>
         */
        public static final String APPWIDGET_PROVIDER = "appWidgetProvider";
        
        /**
         * Indicates whether this favorite is an application-created shortcut or not.
         * If the value is 0, the favorite is not an application-created shortcut, if the
         * value is 1, it is an application-created shortcut.
         * <P>Type: INTEGER</P>
         */
        @Deprecated
        static final String IS_SHORTCUT = "isShortcut";

        /**
         * The URI associated with the favorite. It is used, for instance, by
         * live folders to find the content provider.
         * <P>Type: TEXT</P>
         */
        static final String URI = "uri";

        /**
         * The display mode if the item is a live folder.
         * <P>Type: INTEGER</P>
         *
         * @see android.provider.LiveFolders#DISPLAY_MODE_GRID
         * @see android.provider.LiveFolders#DISPLAY_MODE_LIST
         */
        static final String DISPLAY_MODE = "displayMode";
    }
}
