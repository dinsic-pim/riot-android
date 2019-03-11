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

package fr.gouv.tchap.fragments;

import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;

import org.matrix.androidsdk.data.Room;
import org.matrix.androidsdk.rest.model.RoomMember;
import org.matrix.androidsdk.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import butterknife.BindView;
import im.vector.R;
import im.vector.fragments.AbsHomeFragment;
import im.vector.util.HomeRoomsViewModel;
import im.vector.view.EmptyViewItemDecoration;
import im.vector.view.SimpleDividerItemDecoration;
import fr.gouv.tchap.adapters.TchapRoomAdapter;

public class TchapRoomsFragment extends AbsHomeFragment implements AbsHomeFragment.OnRoomChangedListener {
    private static final String LOG_TAG = TchapRoomsFragment.class.getSimpleName();

    @BindView(R.id.recyclerview)
    RecyclerView mRecycler;

    // rooms management
    private TchapRoomAdapter mAdapter;

    // rooms list
    private final List<Room> mRooms = new ArrayList<>();

    /*
     * *********************************************************************************************
     * Static methods
     * *********************************************************************************************
     */

    public static TchapRoomsFragment newInstance() {
        return new TchapRoomsFragment();
    }

    /*
     * *********************************************************************************************
     * Fragment lifecycle
     * *********************************************************************************************
     */

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_rooms, container, false);
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mPrimaryColor = ContextCompat.getColor(getActivity(), R.color.tab_rooms);
        mSecondaryColor = ContextCompat.getColor(getActivity(), R.color.tab_rooms_secondary);

        initViews();

        mOnRoomChangedListener = this;

        // Initialize the filter inputs
        mCurrentFilter = mActivity.getSearchQuery();
        mAdapter.onFilterDone(mCurrentFilter);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (null != mActivity) {
            mAdapter.setInvitation(mActivity.getRoomInvitations());
        }
        mRecycler.addOnScrollListener(mScrollListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        mRecycler.removeOnScrollListener(mScrollListener);
    }

    /*
     * *********************************************************************************************
     * Abstract methods implementation
     * *********************************************************************************************
     */

    @Override
    protected List<Room> getRooms() {
        return new ArrayList<>(mRooms);
    }

    @Override
    protected void onFilter(String pattern, final OnFilterListener listener) {
        mAdapter.getFilter().filter(pattern, new Filter.FilterListener() {
            @Override
            public void onFilterComplete(int count) {
                Log.i(LOG_TAG, "onFilterComplete " + count);
                if (listener != null) {
                    listener.onFilterDone(count);
                }

            }
        });
    }

    @Override
    protected void onResetFilter() {
        mAdapter.getFilter().filter("", new Filter.FilterListener() {
            @Override
            public void onFilterComplete(int count) {
                Log.i(LOG_TAG, "onResetFilter " + count);

            }
        });
    }

    /*
     * *********************************************************************************************
     * Public methods
     * *********************************************************************************************
     */

    @Override
    public void onRoomResultUpdated(final HomeRoomsViewModel.Result result) {
        if (isResumed()) {
            refreshRooms(result.getJoinedRooms());
            mAdapter.setInvitation(mActivity.getRoomInvitations());
        }
    }

    /*
     * *********************************************************************************************
     * UI management
     * *********************************************************************************************
     */

    private void initViews() {
        int margin = (int) getResources().getDimension(R.dimen.item_decoration_left_margin);
        mRecycler.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        mRecycler.addItemDecoration(new SimpleDividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL, margin));
        mRecycler.addItemDecoration(new EmptyViewItemDecoration(getActivity(), DividerItemDecoration.VERTICAL, 40, 16, 14));

        mAdapter = new TchapRoomAdapter(getActivity(), new TchapRoomAdapter.OnSelectItemListener() {
            @Override
            public void onSelectItem(Room room, int position) {
                openRoom(room);
            }

        }, this, this);
        mRecycler.setAdapter(mAdapter);

    }

    /*
     * *********************************************************************************************
     * rooms management
     * *********************************************************************************************
     */

    /**
     * Init the rooms display
     */
    private void refreshRooms(List<Room> allJoinedRooms) {

        mRooms.clear();

        for (Room room : allJoinedRooms) {

            if (room.isDirect()) {
                // TODO handle correctly the lazy loading option by handling asynchronously the room members.
                Collection<RoomMember> members = room.getState().getDisplayableLoadedMembers();
                for (RoomMember member : members) {
                    if (member.getUserId().equals(mSession.getMyUserId())) {
                        continue;
                    }

                    // Hide the rooms created to invite some non-tchap contact by email.
                    // -> Check whether there is no pending 3PID invite for this member.
                    // Hide the direct chat left by the other member.
                    // -> Keep only the direct chat in which the other member has joined or is invited.
                    if (member.getThirdPartyInviteToken() == null
                            && (member.membership.equals(RoomMember.MEMBERSHIP_JOIN) || member.membership.equals(RoomMember.MEMBERSHIP_INVITE))) {
                        mRooms.add(room);
                        // Break here the loop on members in case of a wrong direct chat
                        // (with several members)
                        break;
                    }
                }
            } else {
                mRooms.add(room);
            }
        }
        mAdapter.setRooms(mRooms);
    }

    /*
     * *********************************************************************************************
     * Listeners
     * *********************************************************************************************
     */

    @Override
    public void onToggleDirectChat(String roomId, boolean isDirectChat) {
    }

    @Override
    public void onRoomLeft(String roomId) {
    }

    @Override
    public void onRoomForgot(String roomId) {
        // there is no sync event when a room is forgotten
    }
}
