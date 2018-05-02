/*
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

package fr.gouv.tchap.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.widget.Toast;

import org.matrix.androidsdk.MXSession;
import org.matrix.androidsdk.data.Room;
import org.matrix.androidsdk.data.RoomTag;
import org.matrix.androidsdk.rest.callback.ApiCallback;
import org.matrix.androidsdk.rest.model.MatrixError;
import org.matrix.androidsdk.util.Log;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import im.vector.R;
import im.vector.activity.CommonActivityUtils;
import fr.gouv.tchap.activity.TchapLoginActivity;
import im.vector.activity.RiotAppCompatActivity;
import im.vector.activity.VectorRoomActivity;
import im.vector.activity.VectorRoomCreationActivity;
import im.vector.adapters.ParticipantAdapterItem;
import im.vector.contacts.Contact;
import im.vector.contacts.ContactsManager;
import im.vector.util.RoomUtils;

/**
 * Created by cloud on 1/22/18.
 */

public class DinsicUtils {
    private static final String LOG_TAG = "DinsicUtils";
    public static final String AGENT_OR_INTERNAL_SECURED_EMAIL_HOST = "gouv.fr";
    /**
     * Tells if an email is from gov
     *
     * @param email the email address to test.
     * @return true if the email address is from gov
     */
    public  static boolean isFromFrenchGov(final String email) {
        return (null != email && email.toLowerCase().endsWith (AGENT_OR_INTERNAL_SECURED_EMAIL_HOST));
    }

    /**
     * Tells if one of the email of the list  is from gov
     *
     * @param emails list to test.
     * @return true if one is from gov
     */
    public  static boolean isFromFrenchGov(final List<String> emails) {
        boolean myReturn=false;
        for (String myEmail : emails){
            if (!myReturn) myReturn = isFromFrenchGov(myEmail);
        }
        return myReturn;
    }

    /**
     * Edit contact form
     */
    public static void editContactForm(Context theContext, Activity myActivity, String editContactWarningMsg, final Contact contact ) {
        LayoutInflater inflater = LayoutInflater.from(theContext);

        Cursor namesCur=null;
        boolean switchToContact=false;
        try
        {
            ContentResolver cr = theContext.getContentResolver();
            namesCur = cr.query(ContactsContract.Data.CONTENT_URI,
                    new String[]{ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                            ContactsContract.Contacts.LOOKUP_KEY,
                            ContactsContract.CommonDataKinds.StructuredName.CONTACT_ID
                    },
                    ContactsContract.Data.MIMETYPE + " = ? AND "+ ContactsContract.Data.CONTACT_ID + " = ?",

                    new String[]{ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE,
                            contact.getContactId()}, null);
            if (namesCur != null) {
                if (namesCur.moveToNext()) {
                    String contactId = namesCur.getString(namesCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.CONTACT_ID));
                    Uri mSelectedContactUri;
                    int mLookupKeyIndex;
                    int mIdIndex;
                    String mCurrentLookupKey;
                    mLookupKeyIndex = namesCur.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY);
                    mCurrentLookupKey = namesCur.getString(mLookupKeyIndex);
                    //mCursor.getColumnIndex(ContactsContract.Contacts._ID);
                    long mCurrentId = Integer.parseInt(contactId);//namesCur.getLong(mIdIndex);
                    mSelectedContactUri =
                            ContactsContract.Contacts.getLookupUri(mCurrentId, mCurrentLookupKey);


                    Intent editIntent = new Intent(Intent.ACTION_EDIT);
                    editIntent.setDataAndType(mSelectedContactUri, ContactsContract.Contacts.CONTENT_ITEM_TYPE);
                    editIntent.putExtra("finishActivityOnSaveCompleted", true);
                    myActivity.startActivity(editIntent);
                    switchToContact=true;
                }
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "## editContactForm(): Exception - Msg=" + e.getMessage());
        }
        if (!switchToContact){
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(myActivity);
            alertDialogBuilder.setMessage(editContactWarningMsg);

            // set dialog message
            alertDialogBuilder
                    .setCancelable(false)
                    .setPositiveButton(R.string.ok,null);

            // create alert dialog
            AlertDialog alertDialog = alertDialogBuilder.create();
            // show it
            alertDialog.show();

        }
    }

    public static void editContact(final FragmentActivity activity, final Context theContext,final ParticipantAdapterItem item) {
        if (ContactsManager.getInstance().isContactBookAccessAllowed()) {
            //enterEmailAddress(item.mContact);
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);
            alertDialogBuilder.setMessage(activity.getString(R.string.people_invalid_warning_msg));
            // set dialog message
            alertDialogBuilder
                    .setNegativeButton(R.string.cancel,null)
                    .setPositiveButton(R.string.action_edit_contact_form,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    DinsicUtils.editContactForm(theContext,activity,activity.getString(R.string.people_edit_contact_warning_msg),item.mContact);
                                }
                            });

            // create alert dialog
            AlertDialog alertDialog = alertDialogBuilder.create();
            // show it
            alertDialog.show();

        }
        else {

            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);
            alertDialogBuilder.setMessage(activity.getString(R.string.people_invalid_warning_msg));

            // set dialog message
            alertDialogBuilder
                    .setCancelable(false)
                    .setPositiveButton(R.string.ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {

                                }
                            });

            // create alert dialog
            AlertDialog alertDialog = alertDialogBuilder.create();
            // show it
            alertDialog.show();

        }
    }  

    public static boolean participantAlreadyAdded(List<ParticipantAdapterItem> participants, ParticipantAdapterItem participant){

        boolean find = false;
        Iterator<ParticipantAdapterItem> iterator = participants.iterator();
        boolean finish = !iterator.hasNext();
        while (!finish){
            ParticipantAdapterItem curp = iterator.next();
            if (curp!= null)
                find = curp.mUserId.equals(participant.mUserId);
            finish = (find || !(iterator.hasNext()));
        }

        return find;

    }

    public static boolean removeParticipantIfExist(List<ParticipantAdapterItem> participants, ParticipantAdapterItem participant){

        boolean find = false;
        Iterator<ParticipantAdapterItem> iterator = participants.iterator();
        boolean finish = !iterator.hasNext();
        while (!finish){
            ParticipantAdapterItem curp = iterator.next();
            if (curp!= null) {
                find = curp.mUserId.equals(participant.mUserId);
                if (find) iterator.remove();
            }
            finish = (find || !(iterator.hasNext()));
        }

        return find;
    }

    public static void alertSimpleMsg(FragmentActivity activity, String msg){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);
        alertDialogBuilder.setMessage(msg);

        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {

                            }
                        });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();
        // show it
        alertDialog.show();

    }

    //=============================================================================================
    // Handle existing direct chat room
    //=============================================================================================

    /**
     * Select the most suitable direct chat for the provided user identifier (matrix or third party id).
     * During this search, the pending invite are considered too.
     * The selected room (if any) may be opened synchronously or not. It depends if the room is ready or not.
     *
     * @param activity current activity
     * @param participantId : participant id (matrix id ou email)
     * @param session current session
     * @param canCreate create the direct chat if it does not exist.
     * @return boolean that says if the direct chat room is found or not
     */
    public static boolean openDirectChat(final RiotAppCompatActivity activity, String participantId, final MXSession session, boolean canCreate) {
        Room existingRoom = VectorRoomCreationActivity.isDirectChatRoomAlreadyExist(participantId, session, true);
        boolean succeeded = false;

        // direct message api callback
        ApiCallback<String> prepareDirectChatCallBack = new ApiCallback<String>() {
            @Override
            public void onSuccess(final String roomId) {
                activity.stopWaitingView();

                HashMap<String, Object> params = new HashMap<>();
                params.put(VectorRoomActivity.EXTRA_MATRIX_ID, session.getMyUserId());
                params.put(VectorRoomActivity.EXTRA_ROOM_ID, roomId);
                params.put(VectorRoomActivity.EXTRA_EXPAND_ROOM_HEADER, true);

                Log.d(LOG_TAG, "## prepareDirectChatCallBack: onSuccess - start goToRoomPage");
                CommonActivityUtils.goToRoomPage(activity, session, params);
            }

            private void onError(final String message) {
                activity.waitingView.post(new Runnable() {
                    @Override
                    public void run() {
                        if (null != message) {
                            Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
                        }
                        activity.stopWaitingView();
                    }
                });
            }

            @Override
            public void onNetworkError(Exception e) {
                onError(e.getLocalizedMessage());
            }

            @Override
            public void onMatrixError(final MatrixError e) {
                onError(e.getLocalizedMessage());
            }

            @Override
            public void onUnexpectedError(final Exception e) {
                onError(e.getLocalizedMessage());
            }
        };

        if (null != existingRoom) {
            // If I am invited to this room, I accept invitation and join it
            if (existingRoom.isInvited()) {
                succeeded = true;
                activity.showWaitingView();
                session.joinRoom(existingRoom.getRoomId(), prepareDirectChatCallBack);
            } else {
                succeeded = true;
                HashMap<String, Object> params = new HashMap<>();
                params.put(VectorRoomActivity.EXTRA_MATRIX_ID, participantId);
                params.put(VectorRoomActivity.EXTRA_ROOM_ID, existingRoom.getRoomId());
                CommonActivityUtils.goToRoomPage(activity, session, params);
            }
        } else if (canCreate){
            // direct message flow
            //it will be more open on next sprints ...
            if (!TchapLoginActivity.isUserExternal(session)) {
                succeeded = true;
                activity.showWaitingView();
                session.createDirectMessageRoom(participantId, prepareDirectChatCallBack);
            } else {
                DinsicUtils.alertSimpleMsg(activity, activity.getString(R.string.room_creation_forbidden));
            }
        }
        return succeeded;
    }

    /**
     * Prepare a dialog with a selected contact.
     *
     * @param activity  the current activity
     * @param session   the current session
     * @param selectedContact the selected contact
     */
    public static void startDialog(final RiotAppCompatActivity activity, final MXSession session, final ParticipantAdapterItem selectedContact) {
        if (selectedContact.mIsValid) {

            // Tell if contact is tchap user
            if (MXSession.isUserId(selectedContact.mUserId)) { // || DinsicUtils.isFromFrenchGov(item.mContact.getEmails()))
                // The contact is a Tchap user
                if (DinsicUtils.openDirectChat(activity, selectedContact.mUserId, session, false)) {
                    // If a direct chat already exist with him, open it
                    DinsicUtils.openDirectChat(activity, selectedContact.mUserId, session, true);
                } else {
                    // If it's a Tchap user without a direct chat with him
                    // Display a popup to confirm the creation of a new direct chat with him
                    String msg = activity.getResources().getString(R.string.start_new_chat_prompt_msg, selectedContact.mDisplayName);
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);
                    alertDialogBuilder.setMessage(msg);

                    // set dialog message
                    alertDialogBuilder
                            .setCancelable(false)
                            .setPositiveButton(R.string.ok,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            DinsicUtils.openDirectChat(activity, selectedContact.mUserId, session, true);
                                        }
                                    })
                            .setNegativeButton(R.string.cancel, null);

                    // create alert dialog
                    AlertDialog alertDialog = alertDialogBuilder.create();
                    // show it
                    alertDialog.show();
                }
            } else {
                // The contact isn't a Tchap user
                String msg = activity.getResources().getString(R.string.room_invite_non_gov_people);
                if (DinsicUtils.isFromFrenchGov(selectedContact.mContact.getEmails()))
                    msg = activity.getResources().getString(R.string.room_invite_gov_people);

                if (!DinsicUtils.openDirectChat(activity, selectedContact.mUserId, session, false)) {
                    if (TchapLoginActivity.isUserExternal(session)) {
                        DinsicUtils.alertSimpleMsg(activity, activity.getResources().getString(R.string.room_creation_forbidden));
                    } else {
                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);
                        alertDialogBuilder.setMessage(msg);

                        // set dialog message
                        alertDialogBuilder
                                .setCancelable(false)
                                .setPositiveButton(R.string.ok,
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                DinsicUtils.openDirectChat(activity, selectedContact.mUserId, session, true);
                                            }
                                        })
                                .setNegativeButton(R.string.cancel, null);

                        // create alert dialog
                        AlertDialog alertDialog = alertDialogBuilder.create();
                        // show it
                        alertDialog.show();
                    }
                }
            }
        } else { // tell the user that the email must be filled. Propose to fill it
            DinsicUtils.editContact(activity, activity, selectedContact);
        }
    }

    //=============================================================================================
    // Handle Rooms
    //=============================================================================================

    /**
     * Return the Dinsic rooms comparator. We display first the pinned rooms, then we sort them by date.
     *
     * @param session
     * @param reverseOrder
     * @return comparator
     */
    public static Comparator<Room> getRoomsComparator(final MXSession session, final boolean reverseOrder) {
        return new Comparator<Room>() {
            private Comparator<Room> mRoomsDateComparator;

            // Retrieve the default room comparator by date
            private Comparator<Room> getRoomsDateComparator() {
                if (null == mRoomsDateComparator) {
                    mRoomsDateComparator = RoomUtils.getRoomsDateComparator(session, reverseOrder);
                }
                return mRoomsDateComparator;
            }

            public int compare(Room room1, Room room2) {
                // Check first whether some rooms are pinned
                final Set<String> tagsRoom1 = room1.getAccountData().getKeys();
                final boolean isPinnedRoom1 = tagsRoom1 != null && tagsRoom1.contains(RoomTag.ROOM_TAG_FAVOURITE);
                final Set<String> tagsRoom2 = room2.getAccountData().getKeys();
                final boolean isPinnedRoom2 = tagsRoom2 != null && tagsRoom2.contains(RoomTag.ROOM_TAG_FAVOURITE);

                if (isPinnedRoom1 && !isPinnedRoom2) {
                    return reverseOrder ? 1 : -1;
                } else if (!isPinnedRoom1 && isPinnedRoom2) {
                    return reverseOrder ? -1 : 1;
                }
                // Consider the last message date to sort them
                return getRoomsDateComparator().compare(room1, room2);
            }
        };
    }
}
