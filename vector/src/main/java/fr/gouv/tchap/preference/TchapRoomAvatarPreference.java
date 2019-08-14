/*
 * Copyright 2018 New Vector Ltd
 * Copyright 2018 DINSIC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.gouv.tchap.preference;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;

import org.matrix.androidsdk.MXSession;
import org.matrix.androidsdk.data.Room;

import im.vector.util.VectorUtils;

/**
 * Specialized class to target a Room avatar preference.
 * Based on the avatar preference class it redefines refreshAvatar() and
 * add the new method  setConfiguration().
 */
public class TchapRoomAvatarPreference extends HexagonAvatarPreference {

    private Room mRoom;

    public TchapRoomAvatarPreference(Context context) {
        super(context);
    }

    public TchapRoomAvatarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TchapRoomAvatarPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void refreshAvatar() {
        if ((null != mAvatarView) && (null != mRoom)) {
            VectorUtils.loadRoomAvatar(getContext(), mSession, mAvatarView, mRoom);
            // Keep the default settings for the avatar border
        }
    }

    public void setConfiguration(MXSession aSession, Room aRoom) {
        mSession = aSession;
        mRoom = aRoom;
        refreshAvatar();
    }

}