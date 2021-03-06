package com.gr03.amos.bikerapp.Adapters;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.gr03.amos.bikerapp.ChatActivity;
import com.gr03.amos.bikerapp.Models.BasicUser;
import com.gr03.amos.bikerapp.Models.Friend;
import com.gr03.amos.bikerapp.ProfileBasicUserActivity;
import com.gr03.amos.bikerapp.R;
import com.gr03.amos.bikerapp.SaveSharedPreference;

import java.util.ArrayList;

import io.realm.Realm;
import io.realm.RealmRecyclerViewAdapter;
import io.realm.RealmResults;

public class ShowFriendsListRecyclerViewAdapter extends RealmRecyclerViewAdapter<Friend, ShowFriendsListRecyclerViewAdapter.ViewHolder> {

    private RealmResults<Friend> mData;
    private LayoutInflater mInflater;
    private Context context;

    // data is passed into the constructor
    public ShowFriendsListRecyclerViewAdapter(Context context, RealmResults<Friend> data) {
        super(data, true);
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
        this.context = context;
    }

    @Override
    public ShowFriendsListRecyclerViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.friend_list_row, parent, false);
        return new ShowFriendsListRecyclerViewAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ShowFriendsListRecyclerViewAdapter.ViewHolder holder, int position) {
        holder.friendName.setText(mData.get(position).getFirst_name());
        holder.friendMail.setText(mData.get(position).getEmail());
        holder.friendMessage.setOnClickListener(v -> {
            ArrayList<Integer> chatUser = new ArrayList<>();
            chatUser.add(SaveSharedPreference.getUserID(context));
            chatUser.add(Math.toIntExact(mData.get(position).getId()));
            Intent intent = new Intent(context, ChatActivity.class);
            intent.putExtra("chatUser", chatUser);
            context.startActivity(intent);

        });
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public void addFriend(BasicUser user) {
        Realm.init(context);
        Realm realm = Realm.getDefaultInstance();

        mData = realm.where(Friend.class).findAll();
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                Friend newFriend = Friend.getUserAsFriend(user);
                realm.insertOrUpdate(newFriend);
            }
        });
    }


    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView friendName;
        TextView friendMail;
        TextView friendImage;
        ImageButton friendMessage;

        ViewHolder(View itemView) {
            super(itemView);
            friendName = itemView.findViewById(R.id.friend_name);
            friendMail = itemView.findViewById(R.id.friend_mail);
            friendMessage = itemView.findViewById(R.id.friend_message);
//            friendImage = itemView.findViewById(R.id.friend_image_profile);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            int position = getAdapterPosition();
            Long id = mData.get(getAdapterPosition()).getId();
            Intent intent = new Intent(context, ProfileBasicUserActivity.class);
            intent.putExtra("id", id);
            context.startActivity(intent);
        }
    }
}
