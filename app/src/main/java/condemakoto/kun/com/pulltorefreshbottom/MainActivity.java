package condemakoto.kun.com.pulltorefreshbottom;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import kun.conde.pulltorefreshfrombottom.PullToRefreshFromBottom;

public class MainActivity extends AppCompatActivity {

    private TextView textView;
    PullToRefreshFromBottom pullToRefreshFromBottom;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        GridLayoutManager mLayoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setAdapter(new AdapterTest());

        pullToRefreshFromBottom  = (PullToRefreshFromBottom) findViewById(R.id.pullToRefreshView);
        pullToRefreshFromBottom.setPullToRefreshListener(this.pullListener);

        pullToRefreshFromBottom.setPullIndicatorView(R.layout.pull_to_refresh_view);
        View pullArea = pullToRefreshFromBottom.getPullIndicatorView();
        textView = (TextView) pullArea.findViewById(R.id.textView);
        textView.setText("drag to load more data");

        /* Example of how it could be used with custom views.
        View view = new View(this);
        view.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_bright));
        PullToRefreshFromBottom pullToRefreshFromBottom  = (PullToRefreshFromBottom) findViewById(R.id.pullToRefreshView);
        pullToRefreshFromBottom.setPullIndicatorView(view);
        */
    }

    private final PullToRefreshFromBottom.PullToRefreshListener pullListener = new PullToRefreshFromBottom.PullToRefreshListener() {
        @Override
        public void onAboveThreshold() {
            textView.setText("release to load more data");
        }

        @Override
        public void onBehindThreshold() {
            textView.setText("drag to load more data");
        }

        @Override
        public void onRefresh() {
            textView.setText("retrieving data...");

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    pullToRefreshFromBottom.cancelRefresh();
                }
            }, 1000);
        }
    };
}
