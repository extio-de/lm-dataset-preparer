package de.extio.lmdatasetprep;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TextUtils {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(TextUtils.class);
	
	private static final List<String> PARAGRAPH_DELIMITERS = List.of("\n\n", "\n", ".", "!", "?");
	
	public static String normalizeText(final String text) {
		String result = text;
		
		// Remove excessive newlines, spaces and tabs
		int cnt = 0;
		do {
			cnt = result.length();
			result = result.replace("\r", "\n");
			result = result.replace("\n\n\n", "\n\n");
			result = result.replace("\t", "");
			result = result.replace("  ", " ");
			result = result.replace(" .", ".");
			result = result.replace(".  ", ". ");
			result = result.replace("\n.", ".");
			result = result.replace(" !", "!");
			result = result.replace("!  ", "! ");
			result = result.replace("\n!", "!");
			result = result.replace(" ?", "?");
			result = result.replace("?  ", "? ");
			result = result.replace("\n?", "?");
			result = result.replace(" \n", "\n");
		} while (result.length() != cnt);
		
		// Remove newlines mid of the sentence
		final StringBuilder sb = new StringBuilder(result);
		for (int i = 1; i < sb.length(); i++) {
			final char c = sb.charAt(i);
			if (c == '\n' && sb.charAt(i - 1) != '.' && sb.charAt(i - 1) != '?' && sb.charAt(i - 1) != '!' && sb.charAt(i - 1) != '\n') {
				sb.setCharAt(i, ' ');
			}
		}
		result = sb.toString();
		
		LOGGER.info("Normalizer stats: Before: {} After {}", text.length(), result.length());
		
		return result;
	}
	
	public static String normalizeModelResponse(final String response, final boolean removePreamble) {
		var result = StringUtils.replace(response, "\r", "");
		
		if (removePreamble) {
			final int colon = result.indexOf(':');
			if (colon > -1 && colon < result.length() - 2 && (colon < 50 || result.charAt(colon + 1) == '\n')) {
				result = result.substring(colon + 1);
			}
		}
		
		result = StringUtils.trim(result);
		result = StringUtils.strip(result, "´`'“”„‟«»\"\r\n");
		result = StringUtils.trim(result);
		
		return result;
	}
	
	public static List<String> splitParagraphs(final String text, final int chunks_norm, final int chunks_var, final boolean slidingWindows) {
		final int CHUNKS_MIN = chunks_norm - chunks_var;
		final int CHUNKS_MAX = chunks_norm + chunks_var;
		
		if (text.length() <= CHUNKS_MAX) {
			return Collections.singletonList(text);
		}
		
		final List<String> splits = new ArrayList<>();
		
		int pos = 0;
		do {
			int next = -1;
			int delLength = 1;
			if (text.length() - pos > CHUNKS_MAX) {
				for (final String delimiter : PARAGRAPH_DELIMITERS) {
					final int oForw = text.substring(Math.min(pos + chunks_norm, text.length()), Math.min(pos + CHUNKS_MAX, text.length())).indexOf(delimiter);
					final int oBack = text.substring(Math.min(pos + CHUNKS_MIN, text.length()), Math.min(pos + chunks_norm, text.length())).lastIndexOf(delimiter);
					int o = -1;
					if (oForw > -1 && oBack == -1) {
						o = pos + chunks_norm + oForw;
					}
					else if (oForw == -1 && oBack > -1) {
						o = pos + CHUNKS_MIN + oBack;
					}
					else if (oForw > -1 && oBack > -1) {
						o = oForw <= (chunks_var - oBack) ? pos + chunks_norm + oForw : pos + CHUNKS_MIN + oBack;
					}
					
					if (o > -1) {
						next = o;
						delLength = delimiter.length();
						break;
					}
				}
			}
			if (next == -1) {
				next = Math.min(pos + CHUNKS_MAX, text.length());
			}
			
			if (slidingWindows && pos > chunks_var) {
				for (final String delimiter : PARAGRAPH_DELIMITERS) {
					final int o = text.indexOf(delimiter, pos - chunks_var);
					if (o > -1 && o < pos - 10) {
						pos = o + delimiter.length();
						break;
					}
				}
			}
			String split = text.substring(pos, next).trim();
			if (!split.endsWith(".") && !split.endsWith("!") && !split.endsWith("?")) {
				split = split + ".";
			}
			splits.add(split);
			
			pos = next + delLength;
		} while (pos < text.length());
		
		LOGGER.info("Created {} splits", splits.size());
		
		return splits;
	}
	
}
