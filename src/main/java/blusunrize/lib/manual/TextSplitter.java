package blusunrize.lib.manual;


import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class TextSplitter
{
	private final Function<String, Integer> width;
	private final int lineWidth;
	private final int pixelsPerLine;
	private final TIntObjectMap<Map<Integer, SpecialManualElement>> specialByAnchor = new TIntObjectHashMap<>();
	private final TIntObjectMap<SpecialManualElement> specialByPage = new TIntObjectHashMap<>();
	private final List<List<String>> entry = new ArrayList<>();
	private final Function<String, String> tokenTransform;
	private final int pixelsPerPage;
	private TIntIntMap pageByAnchor = new TIntIntHashMap();

	public TextSplitter(Function<String, Integer> w, int lineWidthPixel, int pageHeightPixel,
						int pixelsPerLine, Function<String, String> tokenTransform)
	{
		width = w;
		this.lineWidth = lineWidthPixel;
		pixelsPerPage = pageHeightPixel;
		this.pixelsPerLine = pixelsPerLine;
		this.tokenTransform = tokenTransform;
	}

	public TextSplitter(ManualInstance m)
	{
		this(m.fontRenderer::getStringWidth, 120, 179-28, m.fontRenderer.FONT_HEIGHT, (s) -> s);
	}

	public TextSplitter(ManualInstance m, Function<String, String> tokenTransform)
	{
		this(m.fontRenderer::getStringWidth, 120, 179-28, m.fontRenderer.FONT_HEIGHT, tokenTransform);
	}

	public void clearSpecialByPage()
	{
		specialByPage.clear();
	}

	public void clearSpecialByAnchor()
	{
		specialByAnchor.clear();
	}

	public void addSpecialPage(int ref, int offset, SpecialManualElement element)
	{
		if (offset < 0 || (ref != -1 && ref < 0))
		{
			throw new IllegalArgumentException();
		}
		if (!specialByAnchor.containsKey(ref))
		{
			specialByAnchor.put(ref, new HashMap<>());
		}
		specialByAnchor.get(ref).put(offset, element);
	}

	// I added labels to all break statements to make it more readable
	@SuppressWarnings({"UnnecessaryLabelOnBreakStatement", "UnusedLabel"})
	public void split(String in)
	{
		clearSpecialByPage();
		entry.clear();
		String[] wordsAndSpaces = splitWhitespace(in);
		int pos = 0;
		List<String> overflow = new ArrayList<>();
		updateSpecials(-1, 0);
		entry:
		while (pos < wordsAndSpaces.length)
		{
			List<String> page = new ArrayList<>(overflow);
			overflow.clear();
			page:
			while (page.size() < getLinesOnPage(entry.size()) && pos < wordsAndSpaces.length)
			{
				String line = "";
				int currWidth = 0;
				line:
				while (pos < wordsAndSpaces.length && currWidth < lineWidth)
				{
					String token = tokenTransform.apply(wordsAndSpaces[pos]);
					int textWidth = getWidth(token);
					if (currWidth + textWidth < lineWidth || line.length() == 0)
					{
						pos++;
						if (token.equals("<np>"))
						{
							page.add(line);
							break page;
						}
						else if (token.equals("\n"))
						{
							break line;
						}
						else if (token.startsWith("<&") && token.endsWith(">"))
						{
							int id = Integer.parseInt(token.substring(2, token.length() - 1));
							int pageForId = entry.size();
							Map<Integer, SpecialManualElement> specialForId = specialByAnchor.get(id);
							if (specialForId != null && specialForId.containsKey(0))
							{
								if (page.size() > getLinesOnPage(pageForId))
								{
									pageForId++;
								}
							}
							//New page if there is already a special element on this page
							if (updateSpecials(id, pageForId))
							{
								page.add(line);
								pos--;
								break page;
							}
						}
						else if (!Character.isWhitespace(token.charAt(0)) || line.length() != 0)
						{//Don't add whitespace at the start of a line
							line += token;
							currWidth += textWidth;
						}
					}
					else
					{
						break line;
					}
				}
				line = line.trim();
				if (!line.isEmpty())
					page.add(line);
			}
			if (!page.stream().allMatch(String::isEmpty))
			{
				int linesMax = getLinesOnPage(entry.size());
				if (page.size() > linesMax)
				{
					overflow.addAll(page.subList(linesMax, page.size()));
					page = page.subList(0, linesMax - 1);
				}
				entry.add(page);
			}
		}
	}

	private int getWidth(String text)
	{
		switch (text)
		{
			case "<br>":
			case "<np>":
				return 0;
			default:
				if (text.startsWith("<link;"))
				{
					text = text.substring(text.indexOf(';') + 1);
					text = text.substring(text.indexOf(';') + 1, text.lastIndexOf(';'));
				}
				return width.apply(text);
		}
	}

	private int getLinesOnPage(int id)
	{
		int pixels = pixelsPerPage;
		if (specialByPage.containsKey(id))
		{
			pixels = pixelsPerPage - specialByPage.get(id).getPixelsTaken();
		}
		return MathHelper.floor(pixels / (double) pixelsPerLine);
	}

	private boolean updateSpecials(int ref, int page)
	{
		if (specialByAnchor.containsKey(ref))
		{
			TIntObjectMap<SpecialManualElement> specialByPageTmp = new TIntObjectHashMap<>();
			for (Map.Entry<Integer, SpecialManualElement> entry : specialByAnchor.get(ref).entrySet())
			{
				int specialPage = page + entry.getKey();
				if (specialByPage.containsKey(specialPage))
				{
					return true;
				}
				specialByPageTmp.put(specialPage, entry.getValue());
			}
			specialByPage.putAll(specialByPageTmp);
		}
		else if (ref != -1)
		{//Default reference for page 0
			System.out.println("WARNING: Reference " + ref + " was found, but no special pages were registered for it");
		}
		pageByAnchor.put(ref, page);
		return false;
	}

	private String[] splitWhitespace(String in)
	{
		List<String> parts = new ArrayList<>();
		for (int i = 0; i < in.length(); )
		{
			StringBuilder here = new StringBuilder();
			char first = in.charAt(i);
			here.append(first);
			i++;
			for (; i < in.length(); )
			{
				char hereC = in.charAt(i);
				byte action = shouldSplit(first, hereC);
				if ((action & 1) != 0)
				{
					here.append(in.charAt(i));
					i++;
				}
				if ((action & 2) != 0 || (action & 1) == 0)
				{
					break;
				}
			}
			parts.add(here.toString());
		}
		return parts.toArray(new String[0]);
	}

	/**
	 * @return &1: add
	 * &2: end here
	 */
	private byte shouldSplit(char start, char here)
	{
		byte ret = 0b01;
		if (Character.isWhitespace(start) ^ Character.isWhitespace(here))
		{
			ret = 0b10;
		}
		if (here == '<')
		{
			ret = 0b10;
		}
		if (start == '<')
		{
			ret = 0b01;
			if (here == '>')
			{
				ret |= 0b10;
			}
		}
		return ret;
	}

	public List<List<String>> getEntryText()
	{
		return entry;
	}

	public TIntObjectMap<SpecialManualElement> getSpecials()
	{
		return specialByPage;
	}

	public int getPageForAnchor(int anchor)
	{
		return pageByAnchor.get(anchor);
	}
}