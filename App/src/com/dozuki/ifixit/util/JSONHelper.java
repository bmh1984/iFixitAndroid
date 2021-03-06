package com.dozuki.ifixit.util;

import android.text.Spannable;
import android.text.Spanned;
import android.text.style.URLSpan;
import android.util.Log;

import com.dozuki.ifixit.dozuki.model.Site;
import com.dozuki.ifixit.gallery.model.UploadedImageInfo;
import com.dozuki.ifixit.gallery.model.UserImageInfo;
import com.dozuki.ifixit.gallery.model.UserImageList;
import com.dozuki.ifixit.guide_view.model.Guide;
import com.dozuki.ifixit.guide_view.model.GuideInfo;
import com.dozuki.ifixit.guide_view.model.GuidePart;
import com.dozuki.ifixit.guide_view.model.GuideStep;
import com.dozuki.ifixit.guide_view.model.GuideTool;
import com.dozuki.ifixit.guide_view.model.StepImage;
import com.dozuki.ifixit.guide_view.model.StepLine;
import com.dozuki.ifixit.login.model.User;
import com.dozuki.ifixit.topic_view.model.TopicLeaf;
import com.dozuki.ifixit.topic_view.model.TopicNode;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

public class JSONHelper {
   private static final String LEAF_INDICATOR = "TOPICS";

   public static ArrayList<Site> parseSites(String json) {
      ArrayList<Site> sites = new ArrayList<Site>();

      try {
         JSONArray jSites = new JSONArray(json);
         Site site;

         for (int i = 0; i < jSites.length(); i++) {
            site = parseSite(jSites.getJSONObject(i));

            if (site != null) {
               sites.add(site);
            }
         }
      } catch (JSONException e) {
         Log.e("iFixit", "Error parsing sites: " + e);
      }

      return sites;
   }

   private static Site parseSite(JSONObject jSite) throws JSONException {
      Site site = new Site(jSite.getInt("siteid"));

      site.mName = jSite.getString("name");
      site.mDomain = jSite.getString("domain");
      site.mTitle = jSite.getString("title");
      site.mTheme = jSite.getString("theme");
      site.mPublic = !jSite.getBoolean("private");
      site.mDescription = jSite.getString("description");
      site.mAnswers = jSite.getInt("answers") != 0;

      setAuthentication(site, jSite.getJSONObject("authentication"));

      return site;
   }

   private static void setAuthentication(Site site, JSONObject jAuth) throws JSONException {
      site.mStandardAuth = jAuth.has("standard") ? jAuth.getBoolean("standard") : false;

      site.mSsoUrl = jAuth.has("sso") ? jAuth.getString("sso") : null;

      site.mPublicRegistration = jAuth.getBoolean("public-registration");
   }

   /**
    * Guide parsing
    */
   public static Guide parseGuide(String json) throws JSONException {
      JSONObject jGuideInfo = new JSONObject(json);
      JSONObject jGuide = jGuideInfo.getJSONObject("guide");
      JSONArray jSteps = jGuide.getJSONArray("steps");
      JSONArray jTools = jGuide.getJSONArray("tools");
      JSONArray jParts = jGuide.getJSONArray("parts");
      JSONObject jAuthor = jGuide.getJSONObject("author");
      JSONObject jImage = jGuide.getJSONObject("image");
      Guide guide = new Guide(jGuideInfo.getInt("guideid"));

      guide.setTitle(jGuide.getString("title"));
      guide.setTopic(jGuideInfo.getString("topic"));
      guide.setSubject(jGuide.getString("subject"));
      guide.setAuthor(jAuthor.getString("text"));
      guide.setTimeRequired(jGuide.getString("time_required"));
      guide.setDifficulty(jGuide.getString("difficulty"));
      guide.setIntroduction(jGuide.getString("introduction"));
      guide.setIntroImage(jImage.getString("text"));
      guide.setSummary(jGuide.getString("summary"));

      for (int i = 0; i < jSteps.length(); i++) {
         guide.addStep(parseStep(jSteps.getJSONObject(i)));
      }

      for (int i = 0; i < jTools.length(); i++) {
         guide.addTool(parseTool(jTools.getJSONObject(i)));
      }

      for (int i = 0; i < jParts.length(); i++) {
         guide.addPart(parsePart(jParts.getJSONObject(i)));
      }

      return guide;
   }

   private static GuidePart parsePart(JSONObject jPart) throws JSONException {
      return new GuidePart(jPart.getString("text"), jPart.getString("url"),
       jPart.getString("thumbnail"), jPart.getString("notes"));
   }

   private static GuideTool parseTool(JSONObject jTool) throws JSONException {
      return new GuideTool(jTool.getString("text"), jTool.getString("url"),
       jTool.getString("thumbnail"), jTool.getString("notes"));
   }

   private static GuideStep parseStep(JSONObject jStep) throws JSONException {
      JSONArray jLines = jStep.getJSONArray("lines");
      GuideStep step = new GuideStep(jStep.getInt("number"));

      step.setTitle(jStep.getString("title"));

      try {
         JSONObject jMedia = jStep.getJSONObject("media");
         JSONArray jImages = jMedia.getJSONArray("image");

         for (int i = 0; i < jImages.length(); i++) {
            step.addImage(parseImage(jImages.getJSONObject(i)));
         }
      } catch (JSONException e) {
         StepImage image = new StepImage(0);
         image.setOrderby(1);
         image.setText("");
         step.addImage(image);
      }

      for (int i = 0; i < jLines.length(); i++) {
         step.addLine(parseLine(jLines.getJSONObject(i)));
      }

      return step;
   }

   private static StepImage parseImage(JSONObject jImage) throws JSONException {
      StepImage image = new StepImage(jImage.getInt("imageid"));

      // last image doesn't have orderby so this is necessary. API bug?
      try {
         image.setOrderby(jImage.getInt("orderby"));
      } catch (JSONException e) {
         image.setOrderby(1);
      }

      image.setText(jImage.getString("text"));

      return image;
   }

   private static StepLine parseLine(JSONObject jLine) throws JSONException {
      return new StepLine(jLine.getString("bullet"), jLine.getInt("level"),
       jLine.getString("text"));
   }

   /**
    * Topic hierarchy parsing
    */
   public static TopicNode parseTopics(String json) throws JSONException {
      JSONObject jTopics = new JSONObject(json);
      ArrayList<TopicNode> topics = parseTopicChildren(jTopics);
      TopicNode root = null;

      root = new TopicNode();
      root.addAllTopics(topics);

      return root;
   }

   /**
    * Reads through the given JSONObject and adds any topics to the given
    * topic
    */
   private static ArrayList<TopicNode> parseTopicChildren(JSONObject jTopic)
    throws JSONException {
      @SuppressWarnings("unchecked")
      Iterator<String> iterator = jTopic.keys();
      String topicName;
      ArrayList<TopicNode> topics = new ArrayList<TopicNode>();
      TopicNode currentTopic;

      while (iterator.hasNext()) {
         topicName = iterator.next();

         if (topicName.equals(LEAF_INDICATOR)) {
            topics.addAll(parseTopicLeaves(
             jTopic.getJSONArray(LEAF_INDICATOR)));
         }
         else {
            currentTopic = new TopicNode(topicName);
            currentTopic.addAllTopics(parseTopicChildren(
             jTopic.getJSONObject(topicName)));
            topics.add(currentTopic);
         }
      }

      return topics;
   }

   private static ArrayList<TopicNode> parseTopicLeaves(JSONArray jLeaves)
    throws JSONException {
      ArrayList<TopicNode> topics = new ArrayList<TopicNode>();

      for (int i = 0; i < jLeaves.length(); i++) {
         topics.add(new TopicNode(jLeaves.getString(i)));
      }

      return topics;
   }

   /**
    * Topic leaf parsing
    */
   public static TopicLeaf parseTopicLeaf(String json) throws JSONException {
      JSONObject jTopic = new JSONObject(json);
      JSONArray jGuides = jTopic.getJSONArray("guides");
      JSONObject jSolutions = jTopic.getJSONObject("solutions");
      JSONObject jInfo = jTopic.getJSONObject("topic_info");
      TopicLeaf topicLeaf = new TopicLeaf(jInfo.getString("name"));

      for (int i = 0; i < jGuides.length(); i++) {
         topicLeaf.addGuide(parseGuideInfo(jGuides.getJSONObject(i)));
      }

      topicLeaf.setNumSolutions(Integer.parseInt(jSolutions.getString("count")));
      topicLeaf.setSolutionsUrl(jSolutions.getString("url"));

      return topicLeaf;
   }

   private static GuideInfo parseGuideInfo(JSONObject jGuide) {
      try {
         GuideInfo guideInfo = new GuideInfo(jGuide.getInt("guideid"));

         guideInfo.setSubject(jGuide.getString("subject"));
         guideInfo.setImage(jGuide.getString("image_url"));
         guideInfo.setTitle(jGuide.getString("title"));
         guideInfo.setType(jGuide.getString("type"));
         guideInfo.setUrl(jGuide.getString("url"));

         return guideInfo;
      } catch (JSONException e) {
         Log.e("iFixit", "Error parsing guide info: " + e);
         return null;
      }
   }

   /**
    * Parsing list of UserImageInfo.
    */
   public static UserImageList parseUserImages(String json) throws JSONException {
      JSONArray jImages = new JSONArray(json);

      UserImageList userImageList = new UserImageList();

      for (int i = 0; i < jImages.length(); i++) {
         userImageList.addImage((parseUserImageInfo(jImages.getJSONObject(i))));
      }

      return userImageList;
   }

   public static UserImageInfo parseUserImageInfo(JSONObject jImage)
    throws JSONException {
      UserImageInfo userImageInfo = new UserImageInfo();
      userImageInfo.setImageid(jImage.getString("imageid"));
      userImageInfo.setGuid(jImage.getString("guid"));
      userImageInfo.setHeight(jImage.getString("height"));
      userImageInfo.setWidth(jImage.getString("width"));
      userImageInfo.setRatio(jImage.getString("ratio"));

      return userImageInfo;
   }

   public static UploadedImageInfo parseUploadedImageInfo(String image)
    throws JSONException {
      JSONObject jImage = new JSONObject(image);

      UploadedImageInfo userImageInfo = new UploadedImageInfo();
      userImageInfo.setImageid(jImage.getString("imageid"));
      userImageInfo.setGuid(jImage.getString("guid"));

      return userImageInfo;
   }

   /**
    * Login parsing info
    */
   public static User parseLoginInfo(String json) throws JSONException {
      JSONObject jUser = new JSONObject(json);

      User user = new User();
      user.setUserid(jUser.getString("userid"));
      user.setUsername(jUser.getString("username"));
      user.setImageid(jUser.getString("imageid"));
      user.setSession(jUser.getString("session"));

      return user;
   }

   /**
    * Returns the error message contained in the given JSON, or null if one
    * does not exist.
    *
    * e.g. Returns "Guide not found" for:
    * "{"error":true,"msg":"Guide not found"}"
    */
   public static String parseError(String json) {
      String error = null;

      try {
         JSONObject jError = new JSONObject(json);
         if (jError.getBoolean("error")) {
            error = jError.getString("msg");
         }
      }catch(JSONException e) {
      }

      return error;
   }

   /**
    * Removes relative a hrefs
    *
    * TODO: Update domain with the current site's domain.
    *
    * @param spantext (from Html.fromhtml())
    * @return spanned with fixed links
    */
   public static Spanned correctLinkPaths(Spanned spantext) {
      Object[] spans = spantext.getSpans(0, spantext.length(), Object.class);
      for (Object span : spans) {
         int start = spantext.getSpanStart(span);
         int end = spantext.getSpanEnd(span);
         int flags = spantext.getSpanFlags(span);
         if (span instanceof URLSpan) {
            URLSpan urlSpan = (URLSpan) span;
            if (!urlSpan.getURL().startsWith("http")) {
               if (urlSpan.getURL().startsWith("/")) {
                  urlSpan = new URLSpan("http://www.ifixit.com" +
                   urlSpan.getURL());
               } else {
                  urlSpan = new URLSpan("http://www.ifixit.com/" +
                   urlSpan.getURL());
               }
            }
            ((Spannable)spantext).removeSpan(span);
            ((Spannable)spantext).setSpan(urlSpan, start, end, flags);
         }
      }

      return spantext;
   }
}
