package org.liuyk.konghao.engine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.liuyk.konghao.model.IndexPage;
import org.liuyk.konghao.model.VideoCategory;
import org.liuyk.konghao.model.VideoInfo;
import org.liuyk.konghao.model.VideoProcess;

/**
 * ʹ��Jsoup��ܽ���html���ɵ����ݼ�������
 * @author liu_yunke@sina.cn
 *
 */
public class JsoupHtmlAnalysizer implements DataAnalysizer {

	/** ��ַ */
	public static final String HOST = "http://www.konghao.org/";

	/** ��ҳ */
	public static final String INDEX = HOST + "index";

	/** ��ѧ˳����Ƶ */
	public static final String VIDEO_PROCESS = HOST + "video/process";
	
	private static JsoupHtmlAnalysizer analysizer;
	
	private JsoupHtmlAnalysizer() {
	}
	
	public static JsoupHtmlAnalysizer getInstance() {
		if(analysizer == null) {
			synchronized (JsoupHtmlAnalysizer.class) {
				if(analysizer == null) analysizer = new JsoupHtmlAnalysizer();
			}
		}
		return analysizer;
	}
	
	@Override
	public IndexPage analysisIndex() {
		try {
			IndexPage page = new IndexPage();
			Document document = Jsoup.connect(INDEX).get();
			Elements imgContainer = document.select("#roll_img_container");
			List<VideoCategory> images = analysisVideoCategorys(imgContainer);
			page.setRecycleImages(images);
			
			Elements newVideos = document.select("#content_con div[href]");
			List<VideoCategory> videoCategories = analysisLatestCategries(newVideos);
			page.setVideoCategories(videoCategories);
			return page;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public List<VideoCategory> analysisVideoCategorys(Elements elements) {
		List<VideoCategory> images = new ArrayList<VideoCategory>();
		for (Element link : elements.select("a")) {
			String linkHref = link.attr("href");
			VideoCategory image = new VideoCategory();
			image.setLink(linkHref);
			Elements imgElement = link.select("img");
			String imgSrc = imgElement.attr("abs:src");
			image.setImg(imgSrc);
			image.setName(imgElement.attr("alt"));
			images.add(image);
		}
		return images;
	}

	@Override
	public List<VideoCategory> analysisLatestCategries(Elements elements) {
		List<VideoCategory> images = new ArrayList<VideoCategory>();
		for (Element element : elements) {
			String link = element.attr("abs:href");
			Elements imgElement = element.select("img");
			String img = imgElement.attr("abs:src");
			Elements videoIntro = element.select(".index_video_intro p");
			String name = videoIntro.get(0).text();
			String updateTime = videoIntro.get(1).text();
			VideoCategory videoCategory = new VideoCategory(link, img, name, updateTime);
			images.add(videoCategory);
		}
		return images;
	}

	@Override
	public List<VideoInfo> analysisVideos(String url) {
		try {
			Document document = Jsoup.connect(url).get();
			Element videos = document.getElementById("videos");
			if(videos == null) {
				return null;
			}
			Elements trs = videos.select("tbody tr:nth-child(odd)");
			List<VideoInfo> videoInfos = new ArrayList<VideoInfo>();
			for (Element tr : trs) {
				//<td> <a href="/video/1118" class="video_online_href">΢�ſ����Ļ����ͻ����ǵ�����</a>[537�η���] </td>
				Elements tds = tr.getElementsByTag("td");
				Element tdA = tds.get(1);
				VideoInfo vinfo = new VideoInfo();
				Elements a = tdA.getElementsByAttribute("href");
				//����
				String link = a.attr("abs:href");
				vinfo.setLink(link);
				//����
				vinfo.setName(a.text());
				//���ʴ���
				String text = tdA.text();
				int accessCount = fetchAcccessCount(text);
				vinfo.setAccessCount(accessCount);
				//���
				int seq = Integer.parseInt(tds.get(0).text().trim());
				vinfo.setSeq(seq);
				//��Ƶ��С
				String size = tds.get(2).text();
				vinfo.setSize(size);
				//����ʱ��
				String upldateTime = tds.get(3).text();
				vinfo.setUpdateTime(upldateTime.replace("����", ""));
				
				videoInfos.add(vinfo);
			}
			Collections.sort(videoInfos);
			return videoInfos;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private static int fetchAcccessCount(String text) {
		try {
			int leftIndexOf = text.indexOf("[");
			int rightIndexOf = text.indexOf("]");
			return Integer.parseInt(text.substring(leftIndexOf + 1, rightIndexOf).replace("�η���", ""));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
	}

	@Override
	public String analysisPlayUrl(String url) {
		try {
			Element playerEl = Jsoup.connect(url).get().getElementById("player");
			return playerEl.attr("abs:href");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	public List<VideoProcess> analysisVideoProcesses() {
		try {
			Document document = Jsoup.connect(VIDEO_PROCESS).get();
			Elements proVcsElements = document.getElementsByClass("pro_vcs");
			List<VideoProcess> vps = new ArrayList<VideoProcess>();
			for (Element element : proVcsElements) {
				String link = element.attr("abs:href");
				Elements img = element.select(".pro_img img");
				String image = img.attr("abs:src");
				Elements nameIntro = element.getElementsByClass("pro_intro_top");
				String name = nameIntro.select("span").text();
				String[] videoNumAndSize = fetchVideoNumAndSize(nameIntro.text());
				String videoNum = videoNumAndSize[0];
				String size = videoNumAndSize[1];
				
				String intro = element.getElementsByClass("pro_intro_con").text();
				VideoProcess vp = new VideoProcess();
				vp.setImage(image);
				vp.setIntro(intro);
				vp.setLink(link);
				vp.setSize(size);
				vp.setVideoNum(videoNum);
				vp.setName(name);
				vps.add(vp);
			}
			return vps;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * ���ַ����л�ȡ��Ƶר���ܸ������ܴ�С
	 * @param text
	 * (34����Ƶ,Լ371MB)
	 * @return
	 * ��0����ʾ��Ƶ�ܸ�������1����ʾ��Ƶ�ܴ�С
	 */
	private static String[] fetchVideoNumAndSize(String text) {
		int leftIndex = text.indexOf("(");//(��λ��
		int geIndex = text.indexOf("��");//����λ��
		String videoNum = text.substring(leftIndex + 1, geIndex);
		
		int yueIndex = text.indexOf("Լ");//Լ��λ��
		int mIndex = -1;
		boolean isMb = false;
		if (text.contains("MB")) {
			mIndex = text.indexOf("MB");//MB��λ��
			isMb = true;
		} else {
			mIndex = text.indexOf("G");//G��λ��
		}
		String size = text.substring(yueIndex + 1, mIndex).trim() + (isMb ? "MB" : "G");
		return new String[]{videoNum, size};
	}
	
}
