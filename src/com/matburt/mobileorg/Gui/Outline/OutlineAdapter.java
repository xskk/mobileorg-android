package com.matburt.mobileorg.Gui.Outline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Gui.Outline.Theme.DefaultTheme;
import com.matburt.mobileorg.OrgData.OrgContract.OrgData;
import com.matburt.mobileorg.OrgData.OrgFileParser;
import com.matburt.mobileorg.OrgData.OrgNode;
import com.matburt.mobileorg.OrgData.OrgProviderUtils;
import com.matburt.mobileorg.util.OrgNodeNotFoundException;

public class OutlineAdapter extends ArrayAdapter<OrgNode> {

	private ContentResolver resolver;
	
	private ArrayList<Boolean> expanded = new ArrayList<Boolean>();
	int[] levelColors = new int[0];

	private DefaultTheme theme;
	
	public OutlineAdapter(Context context) {
		super(context, R.layout.outline_item);
		this.resolver = context.getContentResolver();
		
		theme = new DefaultTheme();
		levelColors = new int[] { theme.ccLBlue, theme.c3Yellow, theme.ceLCyan,
				theme.c1Red, theme.c2Green, theme.c5Purple, theme.ccLBlue,
				theme.c2Green, theme.ccLBlue, theme.c3Yellow, theme.ceLCyan };
		init();
	}
	
	public void init() {
		clear();
		this.expanded.clear();
		
		Cursor cursor = resolver.query(
				OrgData.buildChildrenUri("-1"),
				OrgData.DEFAULT_COLUMNS, null, null, OrgData.NAME_SORT);
		cursor.moveToFirst();
		
		while(cursor.isAfterLast() == false) {
			try {
				add(new OrgNode(cursor));
			} catch (OrgNodeNotFoundException e) {
			}
			cursor.moveToNext();
		}
		notifyDataSetInvalidated();
	}
	
	// TODO Clean up and refactor to separate class
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		//ViewHolder holder = (ViewHolder) v.getTag();
		
		View row = convertView;
		
		if (row == null)
			row = LayoutInflater.from(getContext()).inflate(
					R.layout.outline_item, parent, false);

		TextView orgItem = (TextView) row.findViewById(R.id.outline_item_title);
		TextView tagsLayout = (TextView) row.findViewById(R.id.outline_item_tags);
		
		orgItem.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
		tagsLayout.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
		
		OrgNode node = getItem(position);
		String todo = node.todo;
		String name = node.name;
		String priority = node.priority;
		String tags = node.tags;
		
		SpannableStringBuilder itemText = new SpannableStringBuilder(name);
		
		if (name.startsWith("COMMENT"))
			itemText.setSpan(new ForegroundColorSpan(Color.GRAY), 0,
					"COMMENT".length(), 0);
		
		if (name.equals("Archive"))
			itemText.setSpan(new ForegroundColorSpan(Color.GRAY), 0,
					"Archive".length(), 0);
		
		 // Handles prefix generated by blockAgendas to indicate separator
		if(name.startsWith(OrgFileParser.BLOCK_SEPARATOR_PREFIX)) {
			itemText.delete(0, OrgFileParser.BLOCK_SEPARATOR_PREFIX.length());
			row.setBackgroundColor(Color.rgb(25, 25, 112));
			orgItem.setGravity(Gravity.CENTER_VERTICAL
					| Gravity.CENTER_HORIZONTAL);
//			holder.orgItem.setTextSize();
		} else {
			//v.setBackgroundColor(NO_SELECTION);
			orgItem.setGravity(Gravity.LEFT);
		}
		
		
		Pattern urlPattern = Pattern.compile("\\[\\[[^\\]]*\\]\\[([^\\]]*)\\]\\]");
		Matcher matcher = urlPattern.matcher(itemText);
		while(matcher.find()) {
			itemText.delete(matcher.start(), matcher.end());
			itemText.insert(matcher.start(), matcher.group(1));
		
			itemText.setSpan(new ForegroundColorSpan(Color.argb(255, 6, 69, 173)),
					matcher.start(), matcher.start() + matcher.group(1).length(), 0);	
			
			matcher = urlPattern.matcher(itemText);
		}
	
		if (priority != null && TextUtils.isEmpty(priority) == false) {
			Spannable prioritySpan = new SpannableString(priority + " ");
			prioritySpan.setSpan(new ForegroundColorSpan(Color.YELLOW), 0,
					priority.length(), 0);
			itemText.insert(0, prioritySpan);
		}
		
		
		itemText.setSpan(
				new ForegroundColorSpan(levelColors[(int) Math
						.abs((node.level) % levelColors.length)]), 0,
				itemText.length(), 0);
		
		
		if(TextUtils.isEmpty(todo) == false) {
			Spannable todoSpan = new SpannableString(todo + " ");
			
			boolean active = OrgProviderUtils.isTodoActive(todo, resolver);
			
			todoSpan.setSpan(new ForegroundColorSpan(active ? theme.c1Red : theme.caLGreen), 0,
					todo.length(), 0);
			itemText.insert(0, todoSpan);
		}
		
		for(int i = 0; i < node.level; i++)
			itemText.insert(0, "   ");
		
		itemText.setSpan(new StyleSpan(Typeface.NORMAL), 0, itemText.length(), 0);
		orgItem.setText(itemText);
		
		if(tags != null && TextUtils.isEmpty(tags) == false) {
			tagsLayout.setTextColor(Color.GRAY);
			tagsLayout.setText(tags);
		} else
			tagsLayout.setVisibility(View.GONE);
		
		return row;
	}
	
	/**
	 * Used as part of the holding pattern.
	 * 
	 * The idea is to save the findViewById()'s into this container object to
	 * speed up the list adapter. setTag() and getTag() are used to bind and
	 * retrieve the container.
	 * 
	 */
	private static class ViewHolder {
		TextView orgItem;
		TextView tagsLayout;
//		TextView dateInfo;
	}


	@Override
	public void add(OrgNode node) {
		super.add(node);
		this.expanded.add(false);
	}

	@Override
	public void insert(OrgNode node, int index) {
		super.insert(node, index);
		this.expanded.add(index, false);
	}
	
	public void insertAll(ArrayList<OrgNode> nodes, int position) {
		Collections.reverse(nodes);
		for(OrgNode node: nodes)
			insert(node, position);
		notifyDataSetInvalidated();
	}

	@Override
	public void remove(OrgNode node) {
		int position = getPosition(node);
		this.expanded.remove(position);
		super.remove(node);
	}

	
	public void collapseExpand(int position) {
		if(this.expanded.get(position))
			collapse(getItem(position), position);
		else
			expand(position);
	}
	
	public void collapse(OrgNode node, int position) {
		int activePos = position + 1;
		while(activePos < this.expanded.size()) {
			if(getItem(activePos).level <= node.level)
				break;
			collapse(getItem(activePos), activePos);
			remove(getItem(activePos));
		}
		this.expanded.set(position, false);
	}
	
	public void expand(int position) {
		OrgNode node = getItem(position);
		insertAll(node.getChildren(resolver), position + 1);
		this.expanded.set(position, true);
	}
	
	public long getNodeId(int position) {
		OrgNode node = getItem(position);
		return node.id;
	}
}
