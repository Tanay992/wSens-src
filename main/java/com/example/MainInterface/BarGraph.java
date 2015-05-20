package com.example.MainInterface;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.BarChart.Type;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.content.Context;
import android.graphics.Color;

public class BarGraph {

	private GraphicalView view;

	private XYSeries foodSeries;
	private XYSeries beverageSeries;
	private XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();

	private XYSeriesRenderer foodRenderer = new XYSeriesRenderer();
	private XYSeriesRenderer beverageRenderer = new XYSeriesRenderer();
	public XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();

	// "#00CC00","#3366FF","Food","Beverage"
	int[] colors = { Color.parseColor("#00CC00"), Color.parseColor("#3366FF") };

	int x;
	String[] labels;

	@SuppressWarnings("deprecation")
	public BarGraph(int x1, String[] xlabel) {

		for(int i=0;i<xlabel.length;i++)
			xlabel[i] = "\t" + xlabel[i] + "\t";
		
		labels = xlabel;
		x = xlabel.length;
		
		foodSeries = new XYSeries("\tFood\t");
		beverageSeries = new XYSeries("\tBeverage\t");

		mDataset.clear();
		mDataset.addSeries(foodSeries);
		mDataset.addSeries(beverageSeries);

		foodRenderer.setColor(colors[0]);
		foodRenderer.setLineWidth(5f);
		beverageRenderer.setColor(colors[1]);
		beverageRenderer.setLineWidth(5f);
		
		mRenderer.setApplyBackgroundColor(true);
		mRenderer.setMarginsColor(Color.argb(0x00, 0x01, 0x01, 0x01));
		mRenderer.setBackgroundColor(Color.TRANSPARENT);
		mRenderer.setZoomEnabled(true);
		mRenderer.setShowAxes(false);
		mRenderer.setPanEnabled(true);
		
		mRenderer.setLegendTextSize(60f);
		mRenderer.setLabelsTextSize(65f);
		
		mRenderer.setXAxisMin(-1);
		mRenderer.setXAxisMax(x+0.5);
		mRenderer.setXLabels(0);
		mRenderer.setYLabels(0);
		mRenderer.setYAxisMin(0);
		
		mRenderer.setExternalZoomEnabled(false);
		mRenderer.setAxesColor(Color.argb(0x00, 0x01, 0x01, 0x01));
		mRenderer.setBarSpacing(40f);
		mRenderer.setLabelsColor(Color.BLACK);
		mRenderer.setBarWidth(45f);
		mRenderer.setPanLimits(new double[] { -1, x, 0, x });

		mRenderer.addSeriesRenderer(foodRenderer);
		mRenderer.addSeriesRenderer(beverageRenderer);
		mRenderer.setMargins(new int[]{10,0,120,0});
		
		//mRenderer.setFitLegend(true);
		//mRenderer.setLegendHeight(20);

		for (int i = 0; i < x; i++) {
			mRenderer.addXTextLabel(i, labels[i]);
		}

	}

	public GraphicalView getView(Context context) {
		view = ChartFactory.getBarChartView(context, mDataset, mRenderer,
				Type.DEFAULT);
		return view;

	}

	public void UpdateActivityCount(int[] food, int[] beverage) {

		foodSeries.clear();
		beverageSeries.clear();
		
		mDataset.clear();
		mDataset.addSeries(foodSeries);
		mDataset.addSeries(beverageSeries);

		for (int i = 0; i < x; i++) {
			foodSeries.add(i, food[i]);
			beverageSeries.add(i, beverage[i]);
		}

		/*
		foodRenderer.setColor(colors[0]);
		foodRenderer.setLineWidth(2);
		foodRenderer.setChartValuesTextAlign(Align.CENTER);

		beverageRenderer.setColor(colors[1]);
		beverageRenderer.setLineWidth(2);
		beverageRenderer.setChartValuesTextAlign(Align.CENTER);
		*/

		for (int i = 0; i < x; i++) {
			mRenderer.addXTextLabel(i, labels[i]);
		}
	}
	
	
	
	
	
	
	public void setmrenderer(){		
		mRenderer.setApplyBackgroundColor(true);
		mRenderer.setMarginsColor(Color.argb(0x00, 0x01, 0x01, 0x01));
		mRenderer.setBackgroundColor(Color.TRANSPARENT);
		mRenderer.setZoomEnabled(true);
		mRenderer.setShowAxes(false);
		mRenderer.setPanEnabled(true);
		mRenderer.setLegendTextSize(30f);
		mRenderer.setExternalZoomEnabled(false);
		mRenderer.setAxesColor(Color.argb(0x00, 0x01, 0x01, 0x01));
		mRenderer.setLabelsTextSize(20f);
		mRenderer.setXAxisMin(-1);
		mRenderer.setXAxisMax(x+1);
		mRenderer.setXLabels(0);
		mRenderer.setYLabels(0);
		/*
		mRenderer.setXAxisMax(15);
		mRenderer.setYAxisMin(0);
		mRenderer.setYAxisMax(50);
		*/
		mRenderer.setBarSpacing(0);
		mRenderer.setLabelsColor(Color.BLACK);
		mRenderer.setBarWidth(20f);
		//mRenderer.setPanLimits(new double[] { -1, x.length, 0, x.length });

	}

}
