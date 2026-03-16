package com.example.fitlink.services;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.fitlink.models.ChatMessage;
import com.example.fitlink.models.Comment;
import com.example.fitlink.models.Group;
import com.example.fitlink.models.User;
import com.example.fitlink.models.Event;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;


/// a service to interact with the Firebase Realtime Database.
/// this class is a singleton, use getInstance() to get an instance of this class
///
/// @see #getInstance()
/// @see FirebaseDatabase
public class DatabaseService {

    /// tag for logging
    ///
    /// @see Log
    private static final String TAG = "DatabaseService";

    /// paths for different data types in the database
    ///
    /// @see DatabaseService#readData(String)
    private static final String USERS_PATH = "users";
    private static final String GROUPS_PATH = "groups";
    private static final String EVENTS_PATH = "events";
    private static final String GROUP_CHATS_PATH = "group_chats";
    /// the instance of this class
    ///
    /// @see #getInstance()
    private static DatabaseService instance;
    /// the reference to the database
    ///
    /// @see DatabaseReference
    /// @see FirebaseDatabase#getReference()
    private final DatabaseReference databaseReference;

    /// use getInstance() to get an instance of this class
    ///
    /// @see DatabaseService#getInstance()
    private DatabaseService() {
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance("https://fitlink-d9534-default-rtdb.europe-west1.firebasedatabase.app/");
        databaseReference = firebaseDatabase.getReference();
    }

    /// get an instance of this class
    ///
    /// @return an instance of this class
    /// @see DatabaseService
    public static DatabaseService getInstance() {
        if (instance == null) {
            instance = new DatabaseService();
        }
        return instance;
    }

    /// write data to the database at a specific path
    ///
    /// @param path     the path to write the data to
    /// @param data     the data to write (can be any object, but must be serializable, i.e. must have a default constructor and all fields must have getters and setters)
    /// @param callback the callback to call when the operation is completed
    /// @see DatabaseCallback
    private void writeData(@NotNull final String path, @NotNull final Object data, final @Nullable DatabaseCallback<Void> callback) {
        readData(path).setValue(data, (error, ref) -> {
            if (error != null) {
                if (callback == null) return;
                callback.onFailed(error.toException());
            } else {
                if (callback == null) return;
                callback.onCompleted(null);
            }
        });
    }


    // region private generic methods
    // to write and read data from the database

    /// remove data from the database at a specific path
    ///
    /// @param path     the path to remove the data from
    /// @param callback the callback to call when the operation is completed
    /// @see DatabaseCallback
    private void deleteData(@NotNull final String path, @Nullable final DatabaseCallback<Void> callback) {
        readData(path).removeValue((error, ref) -> {
            if (error != null) {
                if (callback == null) return;
                callback.onFailed(error.toException());
            } else {
                if (callback == null) return;
                callback.onCompleted(null);
            }
        });
    }

    /// read data from the database at a specific path
    ///
    /// @param path the path to read the data from
    /// @return a DatabaseReference object to read the data from
    /// @see DatabaseReference

    private DatabaseReference readData(@NotNull final String path) {
        return databaseReference.child(path);
    }

    /// get data from the database at a specific path
    ///
    /// @param path     the path to get the data from
    /// @param clazz    the class of the object to return
    /// @param callback the callback to call when the operation is completed
    /// @see DatabaseCallback
    /// @see Class
    private <T> void getData(@NotNull final String path, @NotNull final Class<T> clazz, @NotNull final DatabaseCallback<T> callback) {
        readData(path).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.e(TAG, "Error getting data", task.getException());
                callback.onFailed(task.getException());
                return;
            }
            T data = task.getResult().getValue(clazz);
            callback.onCompleted(data);
        });
    }

    /// get a list of data from the database at a specific path
    ///
    /// @param path     the path to get the data from
    /// @param clazz    the class of the objects to return
    /// @param callback the callback to call when the operation is completed
    private <T> void getDataList(@NotNull final String path, @NotNull final Class<T> clazz, @NotNull final DatabaseCallback<List<T>> callback) {
        readData(path).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.e(TAG, "Error getting data", task.getException());
                callback.onFailed(task.getException());
                return;
            }
            List<T> tList = new ArrayList<>();
            task.getResult().getChildren().forEach(dataSnapshot -> {
                T t = dataSnapshot.getValue(clazz);
                tList.add(t);
            });

            callback.onCompleted(tList);
        });
    }

    /// generate a new id for a new object in the database
    ///
    /// @param path the path to generate the id for
    /// @return a new id for the object
    /// @see String
    /// @see DatabaseReference#push()

    private String generateNewId(@NotNull final String path) {
        return databaseReference.child(path).push().getKey();
    }

    /// run a transaction on the data at a specific path </br>
    /// good for incrementing a value or modifying an object in the database
    ///
    /// @param path     the path to run the transaction on
    /// @param clazz    the class of the object to return
    /// @param function the function to apply to the current value of the data
    /// @param callback the callback to call when the operation is completed
    /// @see DatabaseReference#runTransaction(Transaction.Handler)
    private <T> void runTransaction(@NotNull final String path, @NotNull final Class<T> clazz, @NotNull UnaryOperator<T> function, @NotNull final DatabaseCallback<T> callback) {
        readData(path).runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                T currentValue = currentData.getValue(clazz);
                if (currentValue == null) {
                    currentValue = function.apply(null);
                } else {
                    currentValue = function.apply(currentValue);
                }
                currentData.setValue(currentValue);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                if (error != null) {
                    Log.e(TAG, "Transaction failed", error.toException());
                    callback.onFailed(error.toException());
                    return;
                }
                T result = currentData != null ? currentData.getValue(clazz) : null;
                callback.onCompleted(result);
            }
        });

    }

    /// generate a new id for a new user in the database
    ///
    /// @return a new id for the user
    /// @see #generateNewId(String)
    /// @see User
    public String generateUserId() {
        return generateNewId(USERS_PATH);
    }

    // endregion of private methods for reading and writing data

    // public methods to interact with the database

    // region User Section

    /// create a new user in the database
    ///
    /// @param user     the user object to create
    /// @param callback the callback to call when the operation is completed
    ///                                                                                                                                                              the callback will receive void
    ///                                                                                                                                                            if the operation fails, the callback will receive an exception
    /// @see DatabaseCallback
    /// @see User
    public void createNewUser(@NotNull final User user, @Nullable final DatabaseCallback<Void> callback) {
        writeData(USERS_PATH + "/" + user.getId(), user, callback);
    }

    /// get a user from the database
    ///
    /// @param uid      the id of the user to get
    /// @param callback the callback to call when the operation is completed
    ///                                                                                                                                                               the callback will receive the user object
    ///                                                                                                                                                             if the operation fails, the callback will receive an exception
    /// @see DatabaseCallback
    /// @see User
    public void getUser(@NotNull final String uid, @NotNull final DatabaseCallback<User> callback) {
        getData(USERS_PATH + "/" + uid, User.class, callback);
    }

    /// get all the users from the database
    ///
    /// @param callback the callback to call when the operation is completed
    ///                                                                                                                                                              the callback will receive a list of user objects
    ///                                                                                                                                                            if the operation fails, the callback will receive an exception
    /// @see DatabaseCallback
    /// @see List
    /// @see User
    public void getUserList(@NotNull final DatabaseCallback<List<User>> callback) {
        getDataList(USERS_PATH, User.class, callback);
    }

    /// delete a user from the database
    ///
    /// @param uid      the user id to delete
    /// @param callback the callback to call when the operation is completed
    public void deleteUser(@NotNull final String uid, @Nullable final DatabaseCallback<Void> callback) {
        deleteData(USERS_PATH + "/" + uid, callback);
    }

    /// get a user by email and password
    ///
    /// @param email    the email of the user
    /// @param password the password of the user
    /// @param callback the callback to call when the operation is completed
    ///                                                                                                                                                            the callback will receive the user object
    ///                                                                                                                                                          if the operation fails, the callback will receive an exception
    /// @see DatabaseCallback
    /// @see User
    public void getUserByEmailAndPassword(@NotNull final String email, @NotNull final String password, @NotNull final DatabaseCallback<User> callback) {
        getUserList(new DatabaseCallback<>() {
            @Override
            public void onCompleted(List<User> users) {
                for (User user : users) {
                    if (Objects.equals(user.getEmail(), email) && Objects.equals(user.getPassword(), password)) {
                        callback.onCompleted(user);
                        return;
                    }
                }
                callback.onCompleted(null);
            }

            @Override
            public void onFailed(Exception e) {

            }
        });
    }

    /// check if an email already exists in the database
    ///
    /// @param email    the email to check
    /// @param callback the callback to call when the operation is completed
    public void checkIfEmailExists(@NotNull final String email, @NotNull final DatabaseCallback<Boolean> callback) {
        getUserList(new DatabaseCallback<>() {
            @Override
            public void onCompleted(List<User> users) {
                for (User user : users) {
                    if (Objects.equals(user.getEmail(), email)) {
                        callback.onCompleted(true);
                        return;
                    }
                }
                callback.onCompleted(false);
            }

            @Override
            public void onFailed(Exception e) {

            }
        });
    }

    public void updateUser(@NotNull final User user, @Nullable final DatabaseCallback<Void> callback) {
        runTransaction(USERS_PATH + "/" + user.getId(), User.class, currentUser -> user, new DatabaseCallback<>() {
            @Override
            public void onCompleted(User object) {
                if (callback != null) {
                    callback.onCompleted(null);
                }
            }

            @Override
            public void onFailed(Exception e) {
                if (callback != null) {
                    callback.onFailed(e);
                }
            }
        });
    }

    /// update only the admin status of a user
    ///
    /// @param uid      user id
    /// @param isAdmin  new admin value (true/false)
    /// @param callback result callback
    public void updateUserAdminStatus(@NotNull final String uid, boolean isAdmin, @Nullable final DatabaseCallback<Void> callback) {
        readData(USERS_PATH + "/" + uid + "/isAdmin")
                .setValue(isAdmin, (error, ref) -> {
                    if (error != null) {
                        if (callback != null) callback.onFailed(error.toException());
                    } else {
                        if (callback != null) callback.onCompleted(null);
                    }
                });
    }

    /**
     * Creates a new sports group and updates the creator's user record.
     */
    public void createNewGroup(@NotNull final Group group, @Nullable final DatabaseCallback<Void> callback) {
        String groupId = group.getId();
        if (groupId == null) {
            groupId = generateNewId(GROUPS_PATH);
            group.setId(groupId);
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put(GROUPS_PATH + "/" + groupId, group);
        updates.put(USERS_PATH + "/" + group.getCreatorId() + "/groupIds/" + groupId, true);

        databaseReference.updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (callback != null) callback.onCompleted(null);
            } else {
                if (callback != null) callback.onFailed(task.getException());
            }
        });
    }

    // region Group Section

    /**
     * Retrieves all available groups.
     */
    public void getAllGroups(@NotNull final DatabaseCallback<List<Group>> callback) {
        databaseReference.child(GROUPS_PATH).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Group> groups = new ArrayList<>();
                for (DataSnapshot groupSnapshot : snapshot.getChildren()) {
                    Group group = groupSnapshot.getValue(Group.class);
                    if (group != null) {
                        groups.add(group);
                    }
                }
                callback.onCompleted(groups);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onFailed(error.toException());
            }
        });
    }

    /**
     * Adds a user to a specific group and updates both records atomically.
     */
    public void joinGroup(@NotNull final String groupId, @NotNull final String userId, @Nullable final DatabaseCallback<Void> callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put(GROUPS_PATH + "/" + groupId + "/members/" + userId, true);
        updates.put(USERS_PATH + "/" + userId + "/groupIds/" + groupId, true);

        databaseReference.updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (callback != null) callback.onCompleted(null);
            } else {
                if (callback != null) callback.onFailed(task.getException());
            }
        });
    }

    /**
     * Removes a user from a specific group and updates both records atomically.
     */
    public void leaveGroup(@NotNull final String groupId, @NotNull final String userId, @Nullable final DatabaseCallback<Void> callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put(GROUPS_PATH + "/" + groupId + "/members/" + userId, null);
        updates.put(GROUPS_PATH + "/" + groupId + "/managers/" + userId, null); // חדש: מסיר את המשתמש גם מרשימת המנהלים במידה והיה כזה
        updates.put(USERS_PATH + "/" + userId + "/groupIds/" + groupId, null);

        databaseReference.updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (callback != null) callback.onCompleted(null);
            } else {
                if (callback != null) callback.onFailed(task.getException());
            }
        });
    }
    /**
     * Updates an existing group in the database.
     */
    public void updateGroup(@NotNull final Group group, @Nullable final DatabaseCallback<Void> callback) {
        databaseReference.child(GROUPS_PATH).child(group.getId()).setValue(group)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (callback != null) callback.onCompleted(null);
                    } else {
                        if (callback != null) callback.onFailed(task.getException());
                    }
                });
    }
    /**
     * Updates a user's manager status in a group.
     */
    public void updateGroupManager(@NotNull final String groupId, @NotNull final String userId, boolean isManager, @Nullable final DatabaseCallback<Void> callback) {
        if (isManager) {
            // הוספת מנהל (שמים true תחת מזהה המשתמש)
            databaseReference.child(GROUPS_PATH).child(groupId).child("managers").child(userId).setValue(true)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            if (callback != null) callback.onCompleted(null);
                        } else {
                            if (callback != null) callback.onFailed(task.getException());
                        }
                    });
        } else {
            // הסרת מנהל (מוחקים את מזהה המשתמש מרשימת המנהלים)
            databaseReference.child(GROUPS_PATH).child(groupId).child("managers").child(userId).removeValue()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            if (callback != null) callback.onCompleted(null);
                        } else {
                            if (callback != null) callback.onFailed(task.getException());
                        }
                    });
        }
    }
    /**
     * User requests to join a group (adds to pendingRequests).
     */
    public void requestToJoinGroup(@NotNull final String groupId, @NotNull final String userId, @Nullable final DatabaseCallback<Void> callback) {
        databaseReference.child(GROUPS_PATH).child(groupId).child("pendingRequests").child(userId).setValue(true)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (callback != null) callback.onCompleted(null);
                    } else {
                        if (callback != null) callback.onFailed(task.getException());
                    }
                });
    }

    /**
     * Manager approves a join request (moves from pending to members).
     */
    public void approveJoinRequest(@NotNull final String groupId, @NotNull final String userId, @Nullable final DatabaseCallback<Void> callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put(GROUPS_PATH + "/" + groupId + "/pendingRequests/" + userId, null);
        updates.put(GROUPS_PATH + "/" + groupId + "/members/" + userId, true);
        updates.put(USERS_PATH + "/" + userId + "/groupIds/" + groupId, true);

        databaseReference.updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (callback != null) callback.onCompleted(null);
            } else {
                if (callback != null) callback.onFailed(task.getException());
            }
        });
    }

    /**
     * Manager declines a join request (removes from pending).
     */
    public void declineJoinRequest(@NotNull final String groupId, @NotNull final String userId, @Nullable final DatabaseCallback<Void> callback) {
        databaseReference.child(GROUPS_PATH).child(groupId).child("pendingRequests").child(userId).removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (callback != null) callback.onCompleted(null);
                    } else {
                        if (callback != null) callback.onFailed(task.getException());
                    }
                });
    }

    /**
     * Retrieves a specific group by its ID.
     */
    public void getGroup(@NotNull final String groupId, @NotNull final DatabaseCallback<Group> callback) {
        getData(GROUPS_PATH + "/" + groupId, Group.class, callback);
    }

    /**
     * Creates a new event in the database and adds it to the creator's event list.
     */
    public void createNewEvent(@NotNull final com.example.fitlink.models.Event event, @Nullable final DatabaseCallback<Void> callback) {
        String eventId = event.getId();
        if (eventId == null) {
            eventId = generateNewId(EVENTS_PATH);
            event.setId(eventId);
        }

        Map<String, Object> updates = new HashMap<>();

        // 1. כתיבת נתוני האירוע
        updates.put(EVENTS_PATH + "/" + eventId, event);

        // 2. הוספת האירוע לרשימת האירועים של היוצר
        updates.put(USERS_PATH + "/" + event.getCreatorId() + "/eventIds/" + eventId, true);

        databaseReference.updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (callback != null) callback.onCompleted(null);
            } else {
                if (callback != null) callback.onFailed(task.getException());
            }
        });
    }
    /**
     * Updates an existing event in the database.
     */
    public void updateEvent(@NotNull final Event event, @Nullable final DatabaseCallback<Void> callback) {
        databaseReference.child(EVENTS_PATH).child(event.getId()).setValue(event)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (callback != null) callback.onCompleted(null);
                    } else {
                        if (callback != null) callback.onFailed(task.getException());
                    }
                });
    }
    /**
     * Retrieves a specific event by its ID.
     */
    public void getEvent(@NotNull final String eventId, @NotNull final DatabaseCallback<Event> callback) {
        getData(EVENTS_PATH + "/" + eventId, Event.class, callback);
    }
    /**
     * Retrieves all events associated with a specific group ID.
     */
    public void getEventsByGroupId(@NotNull final String groupId, @NotNull final DatabaseCallback<List<com.example.fitlink.models.Event>> callback) {
        databaseReference.child(EVENTS_PATH).orderByChild("groupId").equalTo(groupId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<com.example.fitlink.models.Event> events = new ArrayList<>();
                        for (DataSnapshot eventSnapshot : snapshot.getChildren()) {
                            com.example.fitlink.models.Event event = eventSnapshot.getValue(com.example.fitlink.models.Event.class);
                            if (event != null) {
                                events.add(event);
                            }
                        }
                        callback.onCompleted(events);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onFailed(error.toException());
                    }
                });
    }
    /**
     * Retrieves all events (both group and independent) from the database.
     */
    public void getAllEvents(@NotNull final DatabaseCallback<List<com.example.fitlink.models.Event>> callback) {
        databaseReference.child(EVENTS_PATH).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<com.example.fitlink.models.Event> events = new ArrayList<>();
                for (DataSnapshot eventSnapshot : snapshot.getChildren()) {
                    com.example.fitlink.models.Event event = eventSnapshot.getValue(com.example.fitlink.models.Event.class);
                    if (event != null) {
                        events.add(event);
                    }
                }
                callback.onCompleted(events);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onFailed(error.toException());
            }
        });
    }
    /**
     * Retrieves all independent events (events not linked to any group).
     */
    public void getAllIndependentEvents(@NotNull final DatabaseCallback<List<com.example.fitlink.models.Event>> callback) {
        databaseReference.child(EVENTS_PATH).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<com.example.fitlink.models.Event> events = new ArrayList<>();
                for (DataSnapshot eventSnapshot : snapshot.getChildren()) {
                    com.example.fitlink.models.Event event = eventSnapshot.getValue(com.example.fitlink.models.Event.class);
                    // מוסיף לרשימה רק אם האירוע עצמאי
                    if (event != null && event.isIndependent()) {
                        events.add(event);
                    }
                }
                callback.onCompleted(events);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onFailed(error.toException());
            }
        });
    }
    /**
     * Deletes a group entirely from the database, including its chat, its events,
     * the events' comments, and removes its reference from all members.
     * Preserves gamification stats for past events.
     */
    public void deleteGroup(@NotNull final String groupId, @Nullable final DatabaseCallback<Void> callback) {
        getGroup(groupId, new DatabaseCallback<Group>() {
            @Override
            public void onCompleted(Group group) {
                if (group == null) {
                    if (callback != null) callback.onFailed(new Exception("Group not found"));
                    return;
                }

                getEventsByGroupId(groupId, new DatabaseCallback<List<Event>>() {
                    @Override
                    public void onCompleted(List<Event> groupEvents) {
                        Map<String, Object> updates = new HashMap<>();

                        updates.put(GROUPS_PATH + "/" + groupId, null);
                        updates.put(GROUP_CHATS_PATH + "/" + groupId, null);

                        if (group.getMembers() != null) {
                            for (String userId : group.getMembers().keySet()) {
                                updates.put(USERS_PATH + "/" + userId + "/groupIds/" + groupId, null);
                            }
                        }

                        if (groupEvents != null) {
                            long currentTime = System.currentTimeMillis();
                            for (Event event : groupEvents) {
                                boolean isPastEvent = event.getEndTimestamp() < currentTime;

                                updates.put(EVENTS_PATH + "/" + event.getId(), null);
                                updates.put("event_comments/" + event.getId(), null);

                                // ניקוי האירוע אצל כל המשתתפים שנרשמו אליו ושימור ההיסטוריה
                                if (event.getParticipants() != null) {
                                    for (String participantId : event.getParticipants().keySet()) {
                                        updates.put(USERS_PATH + "/" + participantId + "/eventIds/" + event.getId(), null);

                                        if (isPastEvent) {
                                            updates.put(USERS_PATH + "/" + participantId + "/pastEventsCount", com.google.firebase.database.ServerValue.increment(1));
                                        }
                                    }
                                }

                                if (event.getCreatorId() != null) {
                                    updates.put(USERS_PATH + "/" + event.getCreatorId() + "/eventIds/" + event.getId(), null);
                                    if (isPastEvent && (event.getParticipants() == null || !event.getParticipants().containsKey(event.getCreatorId()))) {
                                        updates.put(USERS_PATH + "/" + event.getCreatorId() + "/pastEventsCount", com.google.firebase.database.ServerValue.increment(1));
                                    }
                                }
                            }
                        }

                        databaseReference.updateChildren(updates).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                if (callback != null) callback.onCompleted(null);
                            } else {
                                if (callback != null) callback.onFailed(task.getException());
                            }
                        });
                    }

                    @Override
                    public void onFailed(Exception e) {
                        if (callback != null) callback.onFailed(e);
                    }
                });
            }

            @Override
            public void onFailed(Exception e) {
                if (callback != null) callback.onFailed(e);
            }
        });
    }
    /**
     * User joins an event. Updates both the event's participants and the user's event list.
     */
    public void joinEvent(@NotNull final String eventId, @NotNull final String userId, @Nullable final DatabaseCallback<Void> callback) {
        Map<String, Object> updates = new HashMap<>();

        // 1. מוסיף את המשתמש לרשימת המשתתפים באירוע
        updates.put(EVENTS_PATH + "/" + eventId + "/participants/" + userId, true);

        // 2. מוסיף את האירוע לרשימת האירועים של המשתמש
        updates.put(USERS_PATH + "/" + userId + "/eventIds/" + eventId, true);

        databaseReference.updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (callback != null) callback.onCompleted(null);
            } else {
                if (callback != null) callback.onFailed(task.getException());
            }
        });
    }

    /**
     * User leaves an event. Updates both the event's participants and the user's event list.
     */
    public void leaveEvent(@NotNull final String eventId, @NotNull final String userId, @Nullable final DatabaseCallback<Void> callback) {
        Map<String, Object> updates = new HashMap<>();

        // 1. מסיר את המשתמש מרשימת המשתתפים באירוע
        updates.put(EVENTS_PATH + "/" + eventId + "/participants/" + userId, null);

        // 2. מסיר את האירוע מרשימת האירועים של המשתמש
        updates.put(USERS_PATH + "/" + userId + "/eventIds/" + eventId, null);

        databaseReference.updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (callback != null) callback.onCompleted(null);
            } else {
                if (callback != null) callback.onFailed(task.getException());
            }
        });
    }
    /**
     * Deletes an event entirely from the database.
     * If it's a past event, it increments the pastEventsCount for all participants to preserve gamification stats.
     */
    public void deleteEvent(@NotNull final String eventId, @Nullable final DatabaseCallback<Void> callback) {
        getEvent(eventId, new DatabaseCallback<Event>() {
            @Override
            public void onCompleted(Event event) {
                if (event == null) {
                    if (callback != null) callback.onFailed(new Exception("Event not found"));
                    return;
                }

                Map<String, Object> updates = new HashMap<>();

                // שלב 1: מחיקת האירוע עצמו והתגובות
                updates.put(EVENTS_PATH + "/" + eventId, null);
                updates.put("event_comments/" + eventId, null);

                // בדיקה אם זה אירוע עבר שצריך לשמור בהיסטוריה
                boolean isPastEvent = event.getEndTimestamp() < System.currentTimeMillis();

                // שלב 2: ניקוי האירוע מהפרופיל של כל המשתתפים בו (ושמירת ההיסטוריה אם צריך)
                if (event.getParticipants() != null) {
                    for (String userId : event.getParticipants().keySet()) {
                        updates.put(USERS_PATH + "/" + userId + "/eventIds/" + eventId, null);

                        // הקסם: אם האירוע עבר, מוסיפים 1+ למונה של המשתמש בשרת
                        if (isPastEvent) {
                            updates.put(USERS_PATH + "/" + userId + "/pastEventsCount", com.google.firebase.database.ServerValue.increment(1));
                        }
                    }
                }

                // מוודאים שגם היוצר מטופל למקרה שלא היה ברשימת המשתתפים משום מה
                if (event.getCreatorId() != null) {
                    updates.put(USERS_PATH + "/" + event.getCreatorId() + "/eventIds/" + eventId, null);
                    if (isPastEvent && (event.getParticipants() == null || !event.getParticipants().containsKey(event.getCreatorId()))) {
                        updates.put(USERS_PATH + "/" + event.getCreatorId() + "/pastEventsCount", com.google.firebase.database.ServerValue.increment(1));
                    }
                }

                // שלב 3: ביצוע כל העדכונים בבת אחת בצורה בטוחה (Atomic update)
                databaseReference.updateChildren(updates).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (callback != null) callback.onCompleted(null);
                    } else {
                        if (callback != null) callback.onFailed(task.getException());
                    }
                });
            }

            @Override
            public void onFailed(Exception e) {
                if (callback != null) callback.onFailed(e);
            }
        });
    }
    public void sendContactMessage(String name, String email, String message, @Nullable final DatabaseCallback<Void> callback) {
        String messageId = generateNewId("contact_messages");

        Map<String, Object> messageData = new HashMap<>();
        messageData.put("name", name);
        messageData.put("email", email);
        messageData.put("message", message);
        messageData.put("timestamp", System.currentTimeMillis());

        writeData("contact_messages/" + messageId, messageData, callback);
    }
    public void checkIfPhoneExists(String phone, @NonNull final DatabaseCallback<Boolean> callback) {
        databaseReference.child("users").orderByChild("phone").equalTo(phone)
                .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                        callback.onCompleted(snapshot.exists());
                    }

                    @Override
                    public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                        callback.onFailed(error.toException());
                    }
                });
    }
    // --- אזור הפונקציות החדשות של הצ'אט ---
    public void sendGroupMessage(@NotNull String groupId, @NotNull ChatMessage message, @Nullable DatabaseCallback<Void> callback) {
        String msgId = databaseReference.child(GROUP_CHATS_PATH).child(groupId).push().getKey();
        if (msgId != null) {
            message.setMessageId(msgId);
            databaseReference.child(GROUP_CHATS_PATH).child(groupId).child(msgId).setValue(message)
                    .addOnCompleteListener(task -> {
                        if (callback != null) {
                            if (task.isSuccessful()) callback.onCompleted(null);
                            else callback.onFailed(task.getException());
                        }
                    });
        }
    }

    public void listenForGroupMessages(@NotNull String groupId, @NotNull DatabaseCallback<List<ChatMessage>> callback) {
        databaseReference.child(GROUP_CHATS_PATH).child(groupId).orderByChild("timestamp")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<ChatMessage> messages = new ArrayList<>();
                        for (DataSnapshot msgSnapshot : snapshot.getChildren()) {
                            ChatMessage msg = msgSnapshot.getValue(ChatMessage.class);
                            if (msg != null) messages.add(msg);
                        }
                        callback.onCompleted(messages);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onFailed(error.toException());
                    }
                });
    }

    public void deleteGroupMessage(@NotNull String groupId, @NotNull String messageId, @Nullable DatabaseCallback<Void> callback) {
        databaseReference.child(GROUP_CHATS_PATH).child(groupId).child(messageId).removeValue()
                .addOnCompleteListener(task -> {
                    if (callback != null) {
                        if (task.isSuccessful()) callback.onCompleted(null);
                        else callback.onFailed(task.getException());
                    }
                });
    }

    // הוספת תגובה חדשה לאירוע
    public void addEventComment(Comment comment, DatabaseCallback<Void> callback) {
        String commentId = databaseReference.child("event_comments").child(comment.getEventId()).push().getKey();
        if (commentId != null) {
            comment.setId(commentId);
            databaseReference.child("event_comments").child(comment.getEventId()).child(commentId).setValue(comment)
                    .addOnCompleteListener(task -> {
                        if (callback != null) {
                            if (task.isSuccessful()) {
                                callback.onCompleted(null);
                            } else {
                                callback.onFailed(task.getException());
                            }
                        }
                    });
        } else {
            if (callback != null) {
                callback.onFailed(new Exception("Failed to generate comment ID"));
            }
        }
    }

    // שליפת כל התגובות של אירוע מסוים
    public void getEventComments(String eventId, DatabaseCallback<List<Comment>> callback) {
        databaseReference.child("event_comments").child(eventId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Comment> comments = new ArrayList<>();
                        for (DataSnapshot data : snapshot.getChildren()) {
                            Comment comment = data.getValue(Comment.class);
                            if (comment != null) {
                                comments.add(comment);
                            }
                        }
                        if (callback != null) {
                            callback.onCompleted(comments);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (callback != null) {
                            callback.onFailed(error.toException());
                        }
                    }
                });
    }
    /**
     * Admin Function: Cleans up events that ended more than a specified number of months ago.
     * It leverages the existing deleteEvent() function to ensure gamification stats are safely preserved!
     */
    /**
     * Admin Function: Cleans up events that ended before a specified cutoff timestamp.
     * It leverages the existing deleteEvent() function to ensure gamification stats are safely preserved!
     */
    /**
     * Admin Function: Cleans up events that ended before a specified cutoff timestamp.
     * Uses a single-fetch (get) to prevent infinite loops from continuous listeners.
     */
    public void cleanupOldEvents(long cutoffTimestamp, @Nullable final DatabaseCallback<Integer> callback) {
        // קריאה חד-פעמית למסד הנתונים (ולא מאזין רציף)
        databaseReference.child(EVENTS_PATH).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                if (callback != null) callback.onFailed(task.getException());
                return;
            }

            List<Event> events = new ArrayList<>();
            for (DataSnapshot snapshot : task.getResult().getChildren()) {
                Event event = snapshot.getValue(Event.class);
                if (event != null) {
                    events.add(event);
                }
            }

            if (events.isEmpty()) {
                if (callback != null) callback.onCompleted(0);
                return;
            }

            // סינון אירועים ישנים
            List<Event> eventsToDelete = new ArrayList<>();
            for (Event event : events) {
                if (event.getEndTimestamp() > 0 && event.getEndTimestamp() < cutoffTimestamp) {
                    eventsToDelete.add(event);
                }
            }

            if (eventsToDelete.isEmpty()) {
                if (callback != null) callback.onCompleted(0); // אין מה לנקות
                return;
            }

            // מחיקת האירועים הישנים בזה אחר זה (כל אחד שומר על הסטטיסטיקות של משתתפיו)
            int[] completedOperations = {0};
            for (Event event : eventsToDelete) {
                deleteEvent(event.getId(), new DatabaseCallback<Void>() {
                    @Override
                    public void onCompleted(Void object) {
                        checkIfDone();
                    }

                    @Override
                    public void onFailed(Exception e) {
                        checkIfDone(); // ממשיכים הלאה גם אם אירוע אחד נכשל
                    }

                    private void checkIfDone() {
                        completedOperations[0]++;
                        // אם סיימנו לעבור על כל האירועים
                        if (completedOperations[0] == eventsToDelete.size()) {
                            if (callback != null) callback.onCompleted(eventsToDelete.size());
                        }
                    }
                });
            }
        });
    }

    /// callback interface for database operations
    ///
    /// @param <T> the type of the object to return
    /// @see DatabaseCallback#onCompleted(Object)
    /// @see DatabaseCallback#onFailed(Exception)
    public interface DatabaseCallback<T> {
        /// called when the operation is completed successfully
        void onCompleted(T object);

        /// called when the operation fails with an exception
        void onFailed(Exception e);
    }
}