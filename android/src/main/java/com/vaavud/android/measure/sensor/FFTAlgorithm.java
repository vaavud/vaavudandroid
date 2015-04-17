package com.vaavud.android.measure.sensor;

import java.util.ArrayList;
import java.util.List;

public class FFTAlgorithm {

	private int FFTLength, m;

	// Lookup tables. Only need to recompute when size of FFT changes.
	private double[] cos;
	private double[] sin;

	private double[] x, y;

	public FFTAlgorithm(int n) {
		this.FFTLength = n;
		this.m = (int) (Math.log(n) / Math.log(2));

		// Make sure n is a power of 2
		if (n != (1 << m)) {
			throw new RuntimeException("FFT length must be power of 2");
		}

		// precompute tables
		cos = new double[n / 2];
		sin = new double[n / 2];

		for (int i = 0; i < n / 2; i++) {
			cos[i] = Math.cos(-2 * Math.PI * i / n);
			sin[i] = Math.sin(-2 * Math.PI * i / n);
		}

		//
		x = new double[n];
		y = new double[n];
	}

	public List<Float> doFFT(List<Float> realData, Integer dataLength) {

		for (int i = 0; i < FFTLength; i++) {
			if (i < dataLength) {
				x[i] = realData.get(i);
			}
			else {
				x[i] = 0;
			}
			y[i] = 0;
		}

		fft(x, y);

		realData = new ArrayList<Float>();

		for (int i = 0; i < (FFTLength / 2 + 1); i++) {
			realData.add((float) (Math.sqrt(Math.pow(x[i], 2)
					+ Math.pow(y[i], 2)) * 2 / FFTLength));
		}

		return realData;
	}

	private void fft(double[] x, double[] y) {
		int i, j, k, n1, n2, a;
		double c, s, t1, t2;

		// Bit-reverse
		j = 0;
		n2 = FFTLength / 2;
		for (i = 1; i < FFTLength - 1; i++) {
			n1 = n2;
			while (j >= n1) {
				j = j - n1;
				n1 = n1 / 2;
			}
			j = j + n1;

			if (i < j) {
				t1 = x[i];
				x[i] = x[j];
				x[j] = t1;
				t1 = y[i];
				y[i] = y[j];
				y[j] = t1;
			}
		}

		// FFT
		n1 = 0;
		n2 = 1;

		for (i = 0; i < m; i++) {
			n1 = n2;
			n2 = n2 + n2;
			a = 0;

			for (j = 0; j < n1; j++) {
				c = cos[a];
				s = sin[a];
				a += 1 << (m - i - 1);

				for (k = j; k < FFTLength; k = k + n2) {
					t1 = c * x[k + n1] - s * y[k + n1];
					t2 = s * x[k + n1] + c * y[k + n1];
					x[k + n1] = x[k] - t1;
					y[k + n1] = y[k] - t2;
					x[k] = x[k] + t1;
					y[k] = y[k] + t2;
				}
			}
		}
	}
}