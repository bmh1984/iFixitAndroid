package com.dozuki.ifixit.util;

import android.content.Context;

import com.dozuki.ifixit.R;

import java.io.Serializable;


public class APIError implements Serializable {
   private static final long serialVersionUID = 1L;
   public static enum ErrorType {OTHER, INVALID_USER, PARSE, CONNECTION};
   public String mTitle;
   public String mMessage;
   public ErrorType mType;

   public APIError(String title, String message, ErrorType type) {
      mTitle = title;
      mMessage = message;
      mType = type;
   }

   public static APIError getParseError(Context context) {
      return new APIError(context.getString(R.string.parse_error_title),
       context.getString(R.string.parse_error_message), ErrorType.OTHER);
   }

   public static APIError getConnectionError(Context context) {
      return new APIError(context.getString(R.string.no_connection_title),
       context.getString(R.string.no_connection), ErrorType.OTHER);
   }
}
