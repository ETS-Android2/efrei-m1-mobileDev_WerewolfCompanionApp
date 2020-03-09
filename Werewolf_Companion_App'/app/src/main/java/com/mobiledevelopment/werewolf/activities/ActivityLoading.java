package com.mobiledevelopment.werewolf.activities;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.JsonObjectRequest;
import com.mobiledevelopment.werewolf.R;
import com.mobiledevelopment.werewolf.api.API_Util;
import com.mobiledevelopment.werewolf.model.Party;
import com.mobiledevelopment.werewolf.model.Player;
import com.mobiledevelopment.werewolf.model.Role;
import com.mobiledevelopment.werewolf.util.Util;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;


/**
 * Activity of transition between the activities {@link ActivityPartyNew} and {@link ActivityParty}
 * Receives a {@link Party} and a list of {@link Role}.
 * Gives to each {@link Player} of the {@link Party} a {@link Role}.
 * Whn it's all set, launches the Game.
 */
public class ActivityLoading extends AppCompatActivity
{
    private Party party;
    private ArrayList<Role> roles;

    // About the API
    private Map<String, Role> map;
    private boolean lock;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);

        // Receive Data
        Bundle extras = getIntent().getExtras();
        party = (Party) extras.getSerializable(Util.EXTRA_PARTY);
        roles = (ArrayList<Role>) extras.getSerializable(Util.EXTRA_ROLES);


        // Treat Data - A - Order by Names
        // We get the List of Players, not organized
        ArrayList<Player> playersNotOrdered = party.getPlayers();

        // We reset the list of Players in the Party
        party.setPlayers( new ArrayList<Player>() );

        // Order the players by name
        int numberOfPlayers = playersNotOrdered.size();

        for(int i = 0; i < numberOfPlayers; i++)
        {
            // We set a current Player at the index 0
            Player currentPlayer = playersNotOrdered.get(0);

            // We iterate through the Players to get the one with the "lowest" name
            for (Player player : playersNotOrdered)
            {
                int compare = player.getName().compareToIgnoreCase(currentPlayer.getName());
                if(compare < 0)
                {
                    currentPlayer = player;
                }
            }

            // We add the Player with the "lowest" name to the Party's list, and delete it from the disposable list
            party.addPlayer( currentPlayer.getName() );
            playersNotOrdered.remove(currentPlayer);
        }




        // Treat Data - B - Give each Player a Role

        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        boolean isConnected = (activeNetworkInfo != null && activeNetworkInfo.isConnected());

        // If I have access to the Internet : use the API
        if(isConnected)
        {
            setDataAPI();
        }
        // Otherwise : use the Local Random solution
        else
        {
            setDataRandom();
        }
    }


    /**
     * Small code snippet that calls the next Activity
     */
    private void goToNextActivity()
    {
        Intent intent = new Intent(getBaseContext(), ActivityParty.class);
        intent.putExtra(Util.EXTRA_PARTY, party);
        //startActivity(intent);
    }




    /**
     * Set the data using random
     */
    private void setDataRandom()
    {
        List<Role> rolesCopy = roles;

        // We set a Random variable
        Random rand = new Random();

        // Foreach player
        for (Player player : party.getPlayers())
        {
            // We get a random index, and set some variables
            int index = rand.nextInt(rolesCopy.size());
            int i = 0;
            Role roleToManage = null;

            // We search for the role at the given index
            for (Role role : rolesCopy)
            {
                if(i != index)
                {
                    i++;
                }
                else
                {
                    roleToManage = role;
                    break;
                }
            }

            // We set the Player's Role
            player.setRole(roleToManage);

            // We remove the Role from the List
            rolesCopy.remove(roleToManage);
        }


        // Send Data
        goToNextActivity();
    }




    /**
     * Set the data using the API
     */
    private void setDataAPI()
    {
        // We initialize the Volley objects
        // Instantiate the cache
        Cache cache = new DiskBasedCache(getCacheDir(), 1024 * 1024); // 1MB cap
        // Set up the network to use HttpURLConnection as the HTTP client.
        Network network = new BasicNetwork(new HurlStack());
        // Instantiate the RequestQueue with the cache and network.
        RequestQueue requestQueue = new RequestQueue(cache, network);
        // Start the queue
        requestQueue.start();

        // We initialize the map containing the cards codes && Roles
        map = new HashMap<>();
        lock = true;


        System.out.println("Set 1");
        // 1 - request to get a new Deck
        // We create the request
        JsonObjectRequest requestInitialize = API_initializeCards();

        // We add the request to the Queue
        requestQueue.add(requestInitialize);


        System.out.println("Set 2");
        // 2 - request to create the Deck
        // We create the request
        JsonObjectRequest requestCreateDeck = API_createDeck();

        // We add the request to the Queue
        requestQueue.add(requestCreateDeck);

        /*
        for(Map.Entry<String, Role> entry : map.entrySet())
            Log.i("hugues", entry.getKey() + " - " + getResources().getString( entry.getValue().getName()));
        */


        System.out.println("Set 3");
        // 3 - request to assign a role to each player
        // We create the request
        JsonObjectRequest requestAssignRoles = API_assignRoles();

        // We add the request to the Queue
        requestQueue.add(requestAssignRoles);


        // We set a boolean
        boolean allowedToFinish = true;

        // We wait for the last request to end
        // If it takes too much time, we use the Random Method
        while(!lock)
        {
            int waitTime = 100;
            int totalWaitTime = 0;
            int threshold =  5000;

            try
            {
                Thread.sleep(waitTime);

                totalWaitTime += waitTime;

                if(totalWaitTime > threshold)
                {
                    setDataRandom();
                    allowedToFinish = false;
                    lock = false;
                }
            }
            catch(InterruptedException e)
            {
                System.out.println(e);
            }
        }


        // We go to the next activity
        if(allowedToFinish)
        {
            goToNextActivity();
        }
    }




    /**
     * Creates a JSON Request that draws card for the creation of a deck
     * @return The JSON Request that draws card for the creation of a deck
     */
    private JsonObjectRequest API_initializeCards()
    {
        String ID = "new";
        int cardCount = roles.size();

        return new JsonObjectRequest(Request.Method.GET, API_Util.urlDrawCards(ID, cardCount), null,
            // SUCCESS !
            new Response.Listener<JSONObject>()
            {
                @Override
                public void onResponse(JSONObject response)
                {
                    try
                    {
                        System.out.println("BEGIN - 1");
                        // We create a JSON array
                        JSONArray jsonArray = response.getJSONArray(API_Util.JSON_KEY_DECK_CARDS);
                        System.out.println(jsonArray);

                        // We iterate through that array
                        for(int i = 0; i < jsonArray.length(); i++)
                        {
                            // We create a JSON object
                            JSONObject card = jsonArray.getJSONObject(i);

                            // We extract a specific String, and get the Role at index 0
                            String key = card.getString(API_Util.JSON_KEY_DECK_CARDS_CODE);
                            Role value = roles.get(i);

                            // We put the key and value into the map
                            map.put(key, value);
                            System.out.println(key + " " + value + " - " + map.size());
                        }
                        System.out.println("END - 1");
                    }
                    catch (JSONException e)
                    {
                        // We display the error
                        e.printStackTrace();

                        // We call the default method
                        setDataRandom();
                    }
                }
            },
            // FAILURE ...
            new Response.ErrorListener()
            {
                @Override
                public void onErrorResponse(VolleyError error)
                {
                    // We display the error
                    error.printStackTrace();

                    // We call the default method
                    setDataRandom();
                }
            }
        );
    }



    /**
     * Creates a JSON Request for the creation of a Deck
     * @return The JSON Request for the creation of a Deck
     */
    private JsonObjectRequest API_createDeck()
    {
        System.out.println("BEGIN - 2");
        // We create a list containing all the cards codes
        List<String> cardsCodes = new ArrayList<>(map.keySet());


        return new JsonObjectRequest(Request.Method.GET, API_Util.urlPartialDeck(cardsCodes), null,
            // SUCCESS !
            new Response.Listener<JSONObject>()
            {
                @Override
                public void onResponse(JSONObject response)
                {
                    try
                    {
                        // We save the deck's ID
                        party.setPartyID(response.getString(API_Util.JSON_KEY_DECK_ID));
                    }
                    catch (JSONException e)
                    {
                        // We display the error
                        e.printStackTrace();

                        // We call the default method
                        setDataRandom();
                    }
                }
            },
            // FAILURE ...
            new Response.ErrorListener()
            {
                @Override
                public void onErrorResponse(VolleyError error)
                {
                    // We display the error
                    error.printStackTrace();

                    // We call the default method
                    setDataRandom();
                }
            }
        );
    }




    /**
     * Creates a JSON Request that assigns {@link Role} to each {@link Player}
     * @return The JSON Request that draws card for the creation of a deck
     */
    private JsonObjectRequest API_assignRoles()
    {
        System.out.println("Begin 3");
        String ID = party.getPartyID();
        int cardCount = party.numberOfPlayers;

        return new JsonObjectRequest(Request.Method.GET, API_Util.urlDrawCards(ID, cardCount), null,
                // SUCCESS !
                new Response.Listener<JSONObject>()
                {
                    @Override
                    public void onResponse(JSONObject response)
                    {
                        try
                        {
                            // We create a JSON array
                            JSONArray jsonArray = response.getJSONArray(API_Util.JSON_KEY_DECK_CARDS);
                            int index = 0;

                            for (Player player : party.getPlayers())
                            {
                                // We create a JSON object
                                JSONObject card = jsonArray.getJSONObject(index++);

                                // We extract the current code
                                String code = card.getString(API_Util.JSON_KEY_DECK_CARDS_CODE);

                                // We get the role assigned to that code
                                Role roleToAssign = map.get(code);

                                // We set the role to the player
                                player.setRole(roleToAssign);
                            }
                            System.out.println("End 3");
                            Log.i("hugues", "Success !");
                            lock = true;
                        }
                        catch (JSONException e)
                        {
                            // We display the error
                            e.printStackTrace();

                            // We call the default method
                            setDataRandom();
                        }
                    }
                },
                // FAILURE ...
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error)
                    {
                        // We display the error
                        error.printStackTrace();

                        // We call the default method
                        setDataRandom();
                    }
                }
        );
    }
}
