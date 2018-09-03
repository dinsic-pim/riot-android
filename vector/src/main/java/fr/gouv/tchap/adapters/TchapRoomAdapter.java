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
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.chauthai.swipereveallayout.ViewBinderHelper;
import org.matrix.androidsdk.data.Room;
import java.util.ArrayList;
import java.util.List;
import butterknife.BindView;
import butterknife.ButterKnife;
import fr.gouv.tchap.util.DinsicUtils;
import im.vector.R;
import im.vector.adapters.AbsAdapter;
import im.vector.adapters.AdapterSection;
import im.vector.adapters.RoomViewHolder;

public class TchapRoomAdapter extends AbsAdapter {

    private static final String LOG_TAG = TchapRoomAdapter.class.getSimpleName();

    private final ViewBinderHelper binderHelper = new ViewBinderHelper();
    private final AdapterSection<Room> mRoomsSection;

    private final OnSelectItemListener mListener;

    /*
     * *********************************************************************************************
     * Constructor
     * *********************************************************************************************
     */

    public TchapRoomAdapter(final Context context, final OnSelectItemListener listener, final RoomInvitationListener invitationListener, final MoreRoomActionListener moreActionListener) {
        super(context, invitationListener, moreActionListener);

        mListener = listener;

        mRoomsSection = new AdapterSection<Room>(context, context.getString(R.string.rooms_header), -1,
                R.layout.adapter_item_room_view, TYPE_HEADER_DEFAULT, TYPE_UNDEFINED, new ArrayList<Room>(), DinsicUtils.getRoomsComparator(mSession, false)) {
            @Override
            public int getContentViewType(int position) {
                // In order to retrieve the position of the item in the list,
                // we must take into account the header of the section.
                // As the header occupies the 1st position of the items list,
                // we remove 1 from the position of the item in the list.
                if (position - 1 < getFilteredItems().size()) {
                    final Room room = getFilteredItems().get(position - 1);
                    if (room.isDirect()) {
                        return TYPE_ROOM_DIRECT;
                    } else {
                        return TYPE_ROOM;
                    }
                }
                return TYPE_UNDEFINED;
            }
        };
        mRoomsSection.setEmptyViewPlaceholder(context.getString(R.string.no_room_placeholder), context.getString(R.string.no_result_placeholder));

        addSection(mRoomsSection);

        binderHelper.setOpenOnlyOne(true);
    }

    /*
     * *********************************************************************************************
     * Abstract methods implementation
     * *********************************************************************************************
     */

    @Override
    protected RecyclerView.ViewHolder createSubViewHolder(ViewGroup viewGroup, int viewType) {
        final LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());

        switch (viewType) {
            case TYPE_ROOM:
                View itemView = inflater.inflate(R.layout.adapter_item_room_view, viewGroup, false);
                return new RoomViewHolder(itemView);

            case TYPE_ROOM_DIRECT:
                View itemViewD = inflater.inflate(R.layout.adapter_item_direct_room_view, viewGroup, false);
                return new RoomViewHolder(itemViewD);
        }

        return null;
    }

    @Override
    protected void populateViewHolder(int viewType, RecyclerView.ViewHolder viewHolder, int position) {
        switch (viewType) {
            case TYPE_ROOM:
            case TYPE_ROOM_DIRECT:
                final RoomViewHolder roomViewHolder = (RoomViewHolder) viewHolder;
                final Room room = (Room) getItemForPosition(position);
                // Use ViewBindHelper to restore and save the open/close state of the SwipeRevealView
                // put an unique string id as value, can be any string which uniquely define the data
                binderHelper.bind(roomViewHolder.swipeLayout, String.valueOf(room));
                roomViewHolder.populateViews(mContext, mSession, room, false, false, mMoreRoomActionListener);
                roomViewHolder.roomItemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mListener.onSelectItem(room, -1);
                    }
                });
                break;
        }
    }

    @Override
    protected int applyFilter(String pattern) {
        int nbResults = 0;
        nbResults += filterRoomSection(mRoomsSection, pattern);
        return nbResults;
    }

    /*
     * *********************************************************************************************
     * Public methods
     * *********************************************************************************************
     */

    public void setRooms(final List<Room> rooms) {
        mRoomsSection.setItems(rooms, mCurrentFilterPattern);
        if (!TextUtils.isEmpty(mCurrentFilterPattern)) {
            filterRoomSection(mRoomsSection, String.valueOf(mCurrentFilterPattern));
        }
        updateSections();
    }


    /*
     * *********************************************************************************************
     * View holder
     * *********************************************************************************************
     */

    class PublicRoomViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.public_room_avatar)
        ImageView vPublicRoomAvatar;

        @BindView(R.id.public_room_topic)
        TextView vRoomTopic;

        @BindView(R.id.public_room_members_count)
        TextView vPublicRoomsMemberCountTextView;

        private PublicRoomViewHolder(final View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    /*
     * *********************************************************************************************
     * Inner classes
     * *********************************************************************************************
     */

    public interface OnSelectItemListener {
        void onSelectItem(Room item, int position);

    }
}
