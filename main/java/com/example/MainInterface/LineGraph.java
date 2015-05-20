package com.example.MainInterface;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.TimeSeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.BasicStroke;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.content.Context;
import android.graphics.Color;

public class LineGraph {
	static public int seconds;
	private GraphicalView view;

	private TimeSeries dataset;
	private TimeSeries dataset1;
	private XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();

	private XYSeriesRenderer renderer;
	public XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();
	double[] range;

	public LineGraph(String color1, String color2, String name1, String name2, double[] r)
    {
		dataset = new TimeSeries(name1);
		dataset1 = new TimeSeries(name2);

		range = r;
		mDataset.clear();
		mDataset.addSeries(dataset);
		mDataset.addSeries(dataset1);

		renderer = new XYSeriesRenderer();
		renderer.setColor(Color.parseColor(color1));
		renderer.setFillPoints(true);
		renderer.setChartValuesSpacing(10);
		renderer.setLineWidth(8);
		mRenderer.addSeriesRenderer(renderer);

		renderer = new XYSeriesRenderer();
		renderer.setColor(Color.parseColor(color2));
		renderer.setFillPoints(true);
		renderer.setChartValuesSpacing(10);
		renderer.setLineWidth(8);
		if(name2 == "Goal weight\t"){
			renderer.setStroke(BasicStroke.DASHED);
		}
		mRenderer.addSeriesRenderer(renderer);

		mRenderer.setApplyBackgroundColor(true);
		mRenderer.setMarginsColor(Color.argb(0x00, 0x01, 0x01, 0x01));
		mRenderer.setBackgroundColor(Color.TRANSPARENT);
		mRenderer.setRange(r);
		mRenderer.setZoomEnabled(true);
		mRenderer.setShowAxes(false);
		mRenderer.setPanEnabled(true);
		mRenderer.setLegendTextSize(30f);
		mRenderer.setExternalZoomEnabled(false);
		mRenderer.setAxesColor(Color.argb(0x00, 0x01, 0x01, 0x01));
		mRenderer.setLabelsTextSize(20f);
		mRenderer.setMargins(new int[]{10,40,30,10});
	}

	public GraphicalView getView(Context context) {
		view = ChartFactory.getLineChartView(context, mDataset, mRenderer);
		return view;

	}

	public void addPoints(int[] val1, int[] val2) {

		dataset.clear();
		dataset1.clear();
		for (int i = 0; i < val1.length; i++) {
			dataset.add(i, val1[i]);
		}

		for (int i = 0; i < val2.length; i++) {
			dataset1.add(i, val2[i]);
		}
		mRenderer.setXLabels(0);
		mRenderer.setYLabels(0);

		// dataset.add(time, mag);
	}

	public void addGoalPoints(int[] val1, int goal) {
		dataset.clear();
		dataset1.clear();
		for (int i =0; i < val1.length; i++) {
			dataset.add(i, val1[i]);
		}

		for (int i = 0; i < range[1]; i++) {
			dataset1.add(i, goal);
		}

		String[] months = { "", "Dec", "Jan", "Feb", "Mar", "Apr", " May",
				" Jun", " Jul", " Aug", " Sep", " Oct", "Nov" };
		mRenderer.setXLabels(0);
		for (int i = 0; i < 12; i++) {
			mRenderer.addXTextLabel(i, months[i]);
		}

		// dataset.add(time, mag);
	}

	public XYMultipleSeriesRenderer getmRenderer() {
		return mRenderer;
	}

	public void setmRenderer(XYMultipleSeriesRenderer mRenderer) {
		this.mRenderer = mRenderer;
	}

	/*
	 * public void addPoints(AccelerometerData data){
	 * 
	 * dataset.add(data.getTimestamp(), data.getMagnitude()); }
	 * 
	 * 
	 * public Intent getIntent(Context context, float mag, float time) {
	 * 
	 * TimeSeries series = new TimeSeries("mag vs time"); series.add(time, mag);
	 * XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
	 * dataset.addSeries(series);
	 * 
	 * XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();
	 * XYSeriesRenderer renderer = new XYSeriesRenderer();
	 * mRenderer.addSeriesRenderer(renderer);
	 * 
	 * Intent intent = ChartFactory.getLineChartIntent(context, dataset,
	 * mRenderer); return intent; }
	 */
}
