package com.dozuki.ifixit.dozuki.ui;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import com.actionbarsherlock.widget.SearchView;
import com.dozuki.ifixit.MainApplication;
import com.dozuki.ifixit.R;
import com.dozuki.ifixit.dozuki.model.Site;
import com.dozuki.ifixit.login.model.LoginListener;
import com.dozuki.ifixit.login.model.User;
import com.dozuki.ifixit.login.ui.LoginFragment;
import com.dozuki.ifixit.topic_view.ui.TopicsActivity;
import com.dozuki.ifixit.util.APIEndpoint;
import com.dozuki.ifixit.util.APIError;
import com.dozuki.ifixit.util.APIReceiver;
import com.dozuki.ifixit.util.APIService;

import org.holoeverywhere.app.Activity;
import org.holoeverywhere.app.DialogFragment;
import org.holoeverywhere.widget.Button;
import org.holoeverywhere.widget.ListView;

import java.util.ArrayList;

public class SiteListActivity extends Activity
 implements SearchView.OnQueryTextListener, LoginListener {
   private static final String SITE_LIST = "SITE_LIST";

   private Button mSiteListButton;
   private ArrayList<Site> mSiteList;
   private ListView mSiteListView;
   private SearchView mSearchView;

   private APIReceiver mApiReceiver = new APIReceiver() {
      @SuppressWarnings("unchecked")
      public void onSuccess(Object result, Intent intent) {
         mSiteList = (ArrayList<Site>)result;
         setSiteList(mSiteList);
      }

      public void onFailure(APIError error, Intent intent) {
         APIService.getErrorDialog(SiteListActivity.this, error,
          APIService.getSitesIntent(SiteListActivity.this)).show();
      }
   };

   @SuppressWarnings("unchecked")
   @Override
   public void onCreate(Bundle savedInstanceState) {
      getSupportActionBar().hide();

      super.onCreate(savedInstanceState);

      setContentView(R.layout.site_list);

      if (savedInstanceState != null) {
         mSiteList = (ArrayList<Site>)savedInstanceState.getSerializable(
          SITE_LIST);
      }

      if (mSiteList == null) {
         getSiteList();
      }

      mSiteListButton = (Button)findViewById(R.id.list_dialog_btn);
      Typeface btnType = Typeface.createFromAsset(getAssets(), "fonts/ProximaNovaRegular.otf");
      mSiteListButton.setTypeface(btnType);

      mSiteListButton.setOnClickListener(new OnClickListener() {
         public void onClick(View view) {
            /**
             * TODO: It should probably always open up the list dialog even if
             * we don't have the site list yet. Then once we get it we can
             * update the list.
             */
            if (mSiteList != null) {
               showSiteListDialog();
            }
         }
      });

      handleIntent(getIntent());
   }

   @Override
   protected void onNewIntent(Intent intent) {
      setIntent(intent);
      handleIntent(intent);
   }

   private void handleIntent(Intent intent) {
      if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
         String query = intent.getStringExtra(SearchManager.QUERY);
         search(query);
      }
   }

   private void search(String query) {
      String lowerQuery = query.toLowerCase();
      ArrayList<Site> matchedSites = new ArrayList<Site>();

      for (Site site : mSiteList) {
         if (site.search(lowerQuery)) {
            matchedSites.add(site);
         }
      }

      setSiteList(matchedSites);
   }

   private void cancelSearch() {
      setSiteList(mSiteList);
   }

   @Override
   public void onSaveInstanceState(Bundle outState) {
      super.onSaveInstanceState(outState);

      outState.putSerializable(SITE_LIST, mSiteList);
   }

   @Override
   public void onResume() {
      super.onResume();

      IntentFilter filter = new IntentFilter();
      filter.addAction(APIEndpoint.SITES.mAction);
      registerReceiver(mApiReceiver, filter);
   }

   @Override
   public void onPause() {
      super.onPause();

      try {
         unregisterReceiver(mApiReceiver);
      } catch (IllegalArgumentException e) {
         // Do nothing. This happens in the unlikely event that
         // unregisterReceiver has been called already.
      }
   }

   /**
    * Sets the ListView and SearchView so this Activity can proxy searches through.
    */
   protected void setSiteListViews(ListView siteListView, SearchView searchView) {
      mSiteListView = siteListView;
      mSearchView = searchView;

      SearchManager searchManager = (SearchManager)getSystemService(
       Context.SEARCH_SERVICE);

      searchView.setSearchableInfo(searchManager.getSearchableInfo(
       getComponentName()));
      searchView.setIconifiedByDefault(false);
      searchView.setOnQueryTextListener(this);

      setSiteList(mSiteList);
   }

   public boolean onQueryTextChange(String newText) {
      if (newText.length() == 0) {
         cancelSearch();
      } else {
         // Perform search on every key press.
         search(newText);
      }

      return false;
   }

   public boolean onQueryTextSubmit(String query) {
      return false;
   }

   public boolean onClose() {
      return false;
   }

   protected boolean isAlwaysExpanded() {
      return false;
   }

   @Override
   public boolean onKeyUp(int keyCode, KeyEvent event) {
      if (keyCode == KeyEvent.KEYCODE_SEARCH) {
         /**
          * Phones with a hardware search button open up the SearchDialog by
          * default. This overrides that by setting focus on the SearchView.
          * Unfortunately it does not open the soft keyboard as of now.
          */
         mSearchView.requestFocus();
         return true;
      } else {
         return super.onKeyUp(keyCode, event);
      }
   }

   private void setSiteList(ArrayList<Site> sites) {
      if (mSiteListView == null || mSiteList == null) {
         return;
      }

      final SiteListAdapter siteListAdapter = new SiteListAdapter(sites);
      mSiteListView.setAdapter(siteListAdapter);

      mSiteListView.setOnItemClickListener(new OnItemClickListener() {
         @Override
         public void onItemClick(AdapterView<?> arg0, View view, int position,
          long id) {
            Site selectedSite = siteListAdapter.getSiteList().get(position);

            if (selectedSite.mPublic) {
               // If the site is public then come on in!
               launchSite(selectedSite);
            } else {
               promptSiteLogin(selectedSite);
            }
         }
      });
   }

   private void promptSiteLogin(Site site) {
      // Set the site so login will work correctly.
      ((MainApplication)getApplication()).setSite(site);
      LoginFragment.newInstance().show(getSupportFragmentManager());
   }

   private void getSiteList() {
      // Make sure we're not on any site anymore.
      ((MainApplication)getApplication()).setSite(null);
      startService(APIService.getSitesIntent(this));
   }

   private void showSiteListDialog() {
       FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
       ft.addToBackStack(null);

       // Create and show the dialog.
       DialogFragment siteListFragment = SiteListDialogFragment.newInstance();
       siteListFragment.show(ft);
   }

   /**
    * Launches TopicsActivity for the given site.
    */
   private void launchSite(Site site) {
      // The site may already be set.
      if (site != null) {
         MainApplication application = ((MainApplication)getApplication());
         application.setSite(site);
      }

      Intent intent = new Intent(this, TopicsActivity.class);
      startActivity(intent);
   }

   @Override
   public void onLogin(User user) {
      /**
       * Go to the site.
       *
       * Note: The current site has already been set before we tried to login.
       */
      launchSite(null);
   }

   @Override
   public void onCancel() {
      /**
       * Do nothing. We just want the dialog to close like it does automatically.
       */
   }

   @Override
   public void onLogout() {
      /**
       * Do nothing. We don't care about logouts here.
       */
   }
}
