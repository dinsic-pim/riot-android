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

package fr.gouv.tchap.adapters;

import android.content.Context;
import android.support.annotation.CallSuper;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.matrix.androidsdk.data.Room;
import org.matrix.androidsdk.rest.model.publicroom.PublicRoom;
import org.matrix.androidsdk.core.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.gouv.tchap.util.DinsicUtils;
import fr.gouv.tchap.util.HexagonMaskView;
import im.vector.R;
import im.vector.adapters.AbsAdapter;
import im.vector.adapters.AdapterSection;
import im.vector.adapters.PublicRoomsAdapterSection;
import im.vector.util.VectorUtils;

public class TchapPublicRoomAdapter extends AbsAdapter {

    private static final String LOG_TAG = TchapPublicRoomAdapter.class.getSimpleName();

    private static final int TYPE_HEADER_PUBLIC_ROOM = 0;

    private static final int TYPE_PUBLIC_ROOM = 1;

    private final PublicRoomsAdapterSection mPublicRoomsSection;

    private final OnSelectItemListener mListener;

    /*
     * *********************************************************************************************
     * Constructor
     * *********************************************************************************************
     */

    public TchapPublicRoomAdapter(final Context context, final OnSelectItemListener listener) {
        super(context);

        mListener = listener;

        mPublicRoomsSection = new PublicRoomsAdapterSection(context, context.getString(R.string.rooms_directory_header),
                -1, R.layout.adapter_item_public_room_view,
                TYPE_HEADER_PUBLIC_ROOM, TYPE_PUBLIC_ROOM, new ArrayList<PublicRoom>(), null);
        mPublicRoomsSection.setEmptyViewPlaceholder(context.getString(R.string.no_public_room_placeholder), context.getString(R.string.no_result_placeholder));

        // External users can not access to public rooms
        if (!DinsicUtils.isExternalTchapSession(mSession)) {
            addSection(mPublicRoomsSection);
        }
    }

    //no sticker on public room
    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
       int i=0;
    }

    /*
     * *********************************************************************************************
     * Abstract methods implementation
     * *********************************************************************************************
     */

    @Override
    protected RecyclerView.ViewHolder createSubViewHolder(ViewGroup viewGroup, int viewType) {
        final LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());

        View itemView;

        switch (viewType) {
            case TYPE_HEADER_PUBLIC_ROOM:
                // Note: We don't want a section header,
                // that is why the header layout_height is null in xml file.
                itemView = inflater.inflate(R.layout.adapter_section_header_public_room, viewGroup, false);
                return new HeaderViewHolder(itemView);
            case TYPE_PUBLIC_ROOM:
                itemView = inflater.inflate(R.layout.adapter_item_public_room_view, viewGroup, false);
                return new PublicRoomViewHolder(itemView);
        }
        return null;
    }

    @Override
    protected void populateViewHolder(int viewType, RecyclerView.ViewHolder viewHolder, int position) {
        switch (viewType) {
            case TYPE_HEADER_PUBLIC_ROOM:
                // Local header
                final HeaderViewHolder headerViewHolder = (HeaderViewHolder) viewHolder;
                for (Pair<Integer, AdapterSection> adapterSection : getSectionsArray()) {
                    if (adapterSection.first == position) {
                        headerViewHolder.populateViews(adapterSection.second);
                        break;
                    }
                }
                break;
            case TYPE_PUBLIC_ROOM:
                final PublicRoomViewHolder publicRoomViewHolder = (PublicRoomViewHolder) viewHolder;
                final PublicRoom publicRoom = (PublicRoom) getItemForPosition(position);
                publicRoomViewHolder.populateViews(publicRoom);
                break;
        }
    }

    @Override
    protected int applyFilter(String pattern) {
        int nbResults = 0;

        // The public rooms search is done by a server request.
        // The result is also paginated so it make no sense to be done in the adapter

        return nbResults;
    }

    /*
     * *********************************************************************************************
     * Public methods
     * *********************************************************************************************
     */


    public void setPublicRooms(final List<PublicRoom> publicRooms) {
        mPublicRoomsSection.setItems(publicRooms, mCurrentFilterPattern);
        updateSections();
    }

    public void setEstimatedPublicRoomsCount(int estimatedCount) {
        mPublicRoomsSection.setEstimatedPublicRoomsCount(estimatedCount);
    }

    public void setNoMorePublicRooms(boolean noMore) {
        mPublicRoomsSection.setHasMoreResults(noMore);
    }

    /**
     * Add more public rooms to the current list
     *
     * @param publicRooms
     */
    @CallSuper
    public void addPublicRooms(final List<PublicRoom> publicRooms) {
        final List<PublicRoom> newPublicRooms = new ArrayList<>();

        newPublicRooms.addAll(mPublicRoomsSection.getItems());
        newPublicRooms.addAll(publicRooms);
        Collections.sort(newPublicRooms, mComparator);
        mPublicRoomsSection.setItems(newPublicRooms, mCurrentFilterPattern);
        updateSections();
    }
    private static final Comparator<PublicRoom> mComparator = new Comparator<PublicRoom>() {
        @Override
        public int compare(PublicRoom lhs, PublicRoom rhs) {
            return rhs.numJoinedMembers - lhs.numJoinedMembers;
        }
    };

    /*
     * *********************************************************************************************
     * View holder
     * *********************************************************************************************
     */

    class PublicRoomViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.public_room_avatar)
        HexagonMaskView vPublicRoomAvatar;

        @BindView(R.id.public_room_name)
        TextView vPublicRoomName;

        @BindView(R.id.public_room_topic)
        TextView vRoomTopic;

        @BindView(R.id.public_room_members_count)
        TextView vPublicRoomsMemberCountTextView;

        @BindView(R.id.public_room_domain)
        TextView vPublicRoomDomain;

        private PublicRoomViewHolder(final View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        private void populateViews(final PublicRoom publicRoom) {
            if (null == publicRoom) {
                Log.e(LOG_TAG, "## populateViews() : null publicRoom");
                return;
            }

            String roomName = !TextUtils.isEmpty(publicRoom.name) ? publicRoom.name : VectorUtils.getPublicRoomDisplayName(publicRoom);

            // Display the room avatar with a restricted border (all public rooms are not allowed to the external users)
            VectorUtils.loadUserAvatar(mContext, mSession, vPublicRoomAvatar, publicRoom.avatarUrl, publicRoom.roomId, roomName);
            vPublicRoomAvatar.setBorderColor(ContextCompat.getColor(mContext, R.color.restricted_room_avatar_border_color));

            // set the topic
            vRoomTopic.setText(publicRoom.topic);

            // display the room name
            vPublicRoomName.setText(roomName);

            // display the room server host
            if (publicRoom.roomId != null)
            {
                vPublicRoomDomain.setText(DinsicUtils.getHomeServerDisplayNameFromMXIdentifier(publicRoom.roomId));
            }

            // members count
            vPublicRoomsMemberCountTextView.setText(mContext.getResources().getQuantityString(R.plurals.public_room_nb_users,
                    publicRoom.numJoinedMembers, publicRoom.numJoinedMembers));

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.onSelectItem(publicRoom);
                }
            });
        }
    }

    /*
     * *********************************************************************************************
     * Inner classes
     * *********************************************************************************************
     */

    public interface OnSelectItemListener {
        void onSelectItem(Room item, int position);

        void onSelectItem(PublicRoom publicRoom);
    }
}
