package fr.openium.converterexample;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

public class MainActivity extends Activity {
	private static int choice = 1000;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		final EditText value1 = (EditText) findViewById(R.id.question);
		final TextView value2 = (TextView) findViewById(R.id.EditText01);
		final TextView text_1 = (TextView) findViewById(R.id.textView4);
		final TextView text_2 = (TextView) findViewById(R.id.textView3);
		final RadioButton radio1 = (RadioButton) findViewById(R.id.radio0);
		final RadioButton radio2 = (RadioButton) findViewById(R.id.radio1);
		final RadioButton radio3 = (RadioButton) findViewById(R.id.radio2);
		final Button ok = (Button) findViewById(R.id.ok);

		ok.setOnClickListener(new View.OnClickListener() {
			Float value;
			Float answ;

			public void onClick(View v) {
				switch (choice) {
				case 0:
					value = Float.parseFloat(value1.getText().toString());
					answ = (float) (value * 3.28);
					value2.setText("" + answ);
					break;
				case 1:
					value = Float.parseFloat(value1.getText().toString());
					answ = (float) (value * 0.86);
					value2.setText("" + answ);
					break;
				case 2:
					value = Float.parseFloat(value1.getText().toString());
					answ = (float) (value * 1.2);
					value2.setText("" + answ);
					break;
				default:
					reset();
					break;
				}

			}

		});
		radio1.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				choice = 0;
				text_1.setText("Meter  ");
				text_2.setText("Feet  ");
				reset();

			}

		});

		radio2.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				choice = 1;
				text_1.setText("Euro  ");
				text_2.setText("Livre sterling  ");
				reset();

			}
		});

		radio3.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				choice = 2;
				text_1.setText("Liter  ");
				text_2.setText("Pinte  ");
				reset();
			}
		});

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public void reset() {
		((EditText) findViewById(R.id.question)).setText("");
		((TextView) findViewById(R.id.EditText01)).setText("");
	}

}
