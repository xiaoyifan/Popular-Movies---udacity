package com.uchicago.yifan.popmovies;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListView;

import com.uchicago.yifan.popmovies.adapter.GridAdapter;
import com.uchicago.yifan.popmovies.data.MovieContract;
import com.uchicago.yifan.popmovies.model.Movie;
import com.uchicago.yifan.popmovies.queries.FetchMoviesTask;

/**
 * A placeholder fragment containing a simple view.
 */
public class MoviesFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{

    private static final String SELECTED_OFFSET = "selected_offset";
    private int list_position = ListView.INVALID_POSITION;
    private GridAdapter adapter;

    private GridView gridview;

    private static final int MOVIES_LOADER = 0;

    public static final String[] MOVIE_COLUMNS = {
            MovieContract.MovieEntry._ID,
            MovieContract.MovieEntry.COLUMN_MOVIE_ID,
            MovieContract.MovieEntry.COLUMN_TITLE,
            MovieContract.MovieEntry.COLUMN_IMAGE,
            MovieContract.MovieEntry.COLUMN_IMAGE2,
            MovieContract.MovieEntry.COLUMN_OVERVIEW,
            MovieContract.MovieEntry.COLUMN_RATING,
            MovieContract.MovieEntry.COLUMN_DATE,
            MovieContract.MovieEntry.COLUMN_POPULARITY
    };

    public static final int COL_ID = 0;
    public static final int COL_MOVIE_ID = 1;
    public static final int COL_TITLE = 2;
    public static final int COL_IMAGE = 3;
    public static final int COL_IMAGE2 = 4;
    public static final int COL_OVERVIEW = 5;
    public static final int COL_RATING = 6;
    public static final int COL_DATE = 7;
    public static final int COL_POPULARITY = 8;


    private static boolean favorited = false;


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getLoaderManager().initLoader(MOVIES_LOADER, null, this);

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String sortOrder = preferences.getString("sortby", "popularity.desc");

        Loader<Cursor> cursorLoader = null;
        if (favorited == true) {

            cursorLoader = new CursorLoader(getActivity(),
                    MovieContract.FavoriteEntry.CONTENT_URI,
                    MOVIE_COLUMNS,
                    null,
                    null,
                    null);
        }
        else {
            cursorLoader = new CursorLoader(getActivity(),
                    MovieContract.MovieEntry.CONTENT_URI,
                    MOVIE_COLUMNS,
                    null,
                    null,
                    null);

        }

        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        adapter.swapCursor(data);

        if (list_position != ListView.INVALID_POSITION){
            gridview.smoothScrollToPosition(list_position);
        }

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.swapCursor(null);
    }

    public interface Callback {
        /**
         * DetailFragmentCallback for when an item has been selected.
         */
        public void onItemSelected(Movie movie);
    }

    public MoviesFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        if (list_position != ListView.INVALID_POSITION) {
            outState.putInt(SELECTED_OFFSET, list_position);
        }

        super.onSaveInstanceState(outState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        adapter = new GridAdapter(getActivity(),null, 0);

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        gridview = (GridView) rootView.findViewById(R.id.gridview);
        gridview.setAdapter(adapter);

        gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                //Movie movie = (Movie) parent.getItemAtPosition(position);
                Cursor cursor = (Cursor) parent.getItemAtPosition(position);
                if (cursor != null) {
                    ((Callback) getActivity())
                            .onItemSelected(new Movie(cursor));
                }

                list_position = position;
            }
        });

        if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_OFFSET)){
            list_position = savedInstanceState.getInt(SELECTED_OFFSET);
        }

        return rootView;
    }

    @Override
    public void onStart() {

        if (Utility.hasNetworkConnection(getActivity()))
        {
            updateData();
        }

        super.onStart();

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.menu_detail, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            updateData();
            return true;
        }
        else if (id == R.id.action_favorite){
            loadFavorites();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void updateData(){

        favorited = false;

        FetchMoviesTask moviesTask = new FetchMoviesTask(getActivity(), this);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String value = preferences.getString("sortby", "popularity.desc");
        moviesTask.execute(value);

        getLoaderManager().restartLoader(MOVIES_LOADER, null, this);
    }

    public void loadFavorites(){

        favorited = true;

        getLoaderManager().restartLoader(MOVIES_LOADER, null, this);
    }


}
