package com.mobiledevelopment.werewolf.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;

import com.mobiledevelopment.werewolf.R;
import com.mobiledevelopment.werewolf.activities.ActivityParty;
import com.mobiledevelopment.werewolf.activities.ActivityPartyNew;
import com.mobiledevelopment.werewolf.util.Util;


/**
 * A simple {@link Fragment} subclass.
 */
public class FragmentPartyLeave extends Fragment
{
    private ActivityParty parentActivity;

    /**
     * Constructor of the class
     * @param parentActivity Reference to the Activity
     */
    public FragmentPartyLeave(final ActivityParty parentActivity)
    {
        this.parentActivity = parentActivity;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_party_leave, container, false);
    }


    @Override
    public void onResume()
    {
        super.onResume();

        // We get the button
        Button button = parentActivity.findViewById(R.id.PartyButtonLeave);


        // When clicked : launches the Activity a describing a game
        button.setOnClickListener(v -> {
            Intent intent = new Intent(parentActivity.getBaseContext(), ActivityPartyNew.class);
            intent.putExtra(Util.EXTRA_PLAYERS, parentActivity.party.getPlayers());
            startActivity(intent);
        });
    }
}
