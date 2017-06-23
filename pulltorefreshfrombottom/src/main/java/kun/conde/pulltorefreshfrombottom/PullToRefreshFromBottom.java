package kun.conde.pulltorefreshfrombottom;

import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

/**
 * @author Julio Kun
 * @version 0.1
 *          <p>
 *          Custom {@link RelativeLayout} that holds a {@link RecyclerView} and a custom view.
 *          It provides a pull to refresh from bottom feature, showing the custom view.
 *          </p>
 */

public class PullToRefreshFromBottom extends RelativeLayout {

    private RecyclerView recyclerView;
    private View pullIndicatorView;
    private int maxHeight;
    private int pullToRefreshThreshold;
    private boolean aboveThreshold;
    private boolean refreshing;
    private boolean propagateTouchEvent = false;
    private int closeAnimationDuration;
    private PullToRefreshListener listener;

    private final static int DEFAULT_CLOSE_ANIMATION_DURATION = 500;

    //region constructors
    public PullToRefreshFromBottom(Context context) {
        super(context);
        init(context, null);
    }

    public PullToRefreshFromBottom(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public PullToRefreshFromBottom(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @TargetApi(21)
    public PullToRefreshFromBottom(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        for (int i = 0; i < getChildCount(); i++) {
            View view = getChildAt(i);
            if (view instanceof RecyclerView) {
                this.recyclerView = (RecyclerView) view;
            }
        }

        this.listener = defaultListener;

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.PullToRefreshView,
                0, 0);

        try {
            this.closeAnimationDuration = a.getInteger(R.styleable.PullToRefreshView_closeAnimationDuration, DEFAULT_CLOSE_ANIMATION_DURATION);
            this.maxHeight = a.getDimensionPixelSize(R.styleable.PullToRefreshView_maxHeight, (int) getResources().getDimension(R.dimen.pull_area_height));
            this.pullToRefreshThreshold = a.getDimensionPixelSize(R.styleable.PullToRefreshView_pullThresholdToRefresh, (int) getResources().getDimension(R.dimen.pull_threshold_height));
        } finally {
            a.recycle();
        }
    }

    @Override
    public void onViewAdded(View child) {
        super.onViewAdded(child);

        if (child instanceof RecyclerView) {
            this.recyclerView = (RecyclerView) child;
            this.recyclerView.setOnTouchListener(this.onTouchListener);
        } else {
            this.pullIndicatorView = child;
        }
    }

    //endregion

    public void setPullIndicatorView(View pullIndicatorView) {
        if (this.pullIndicatorView != null) {
            removeView(this.pullIndicatorView);
        }
        this.pullIndicatorView = pullIndicatorView;
        addView(pullIndicatorView);

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) this.pullIndicatorView.getLayoutParams();
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        params.width = LayoutParams.MATCH_PARENT;
        params.height = 0;
        this.pullIndicatorView.setLayoutParams(params);

        if (recyclerView != null) {
            params = (RelativeLayout.LayoutParams) recyclerView.getLayoutParams();
            params.width = LayoutParams.MATCH_PARENT;
            params.height = LayoutParams.MATCH_PARENT;
            if (pullIndicatorView.getId() <= 0) {
                pullIndicatorView.setId(R.id.pullToRefreshView);
            }
            params.addRule(RelativeLayout.ABOVE, pullIndicatorView.getId());
        } else {
            throw new IllegalArgumentException("A RecyclerView is needed as a child first.");
        }
    }

    public void setPullIndicatorView(int resourceId) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View view = inflater.inflate(resourceId, this, false);
        setPullIndicatorView(view);
    }

    public View getPullIndicatorView() {
        return this.pullIndicatorView;
    }

    public void setRefreshableAreaHeight(int maxHeight) {
        this.maxHeight = maxHeight;
    }

    public void setRefreshableThreshod(int thresholdHeight) {
        this.pullToRefreshThreshold = thresholdHeight;
    }

    public void setPullToRefreshListener(PullToRefreshListener listener) {
        this.listener = listener;
    }

    public void cancelRefresh() {
        refreshing = false;
        //setRefreshableViewHeight(0);
        closeRefreshView();
        propagateTouchEvent = false;
    }

    private void setRefreshableViewHeight(float height) {

        if (height > maxHeight) {
            height = maxHeight;
        }

        if (height >= pullToRefreshThreshold) {
            if (!aboveThreshold) {
                aboveThreshold = true;
                listener.onAboveThreshold();
            }
        } else {
            if (aboveThreshold) {
                aboveThreshold = false;
                listener.onBehindThreshold();
            }
        }

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) pullIndicatorView.getLayoutParams();
        params.height = (int) height;
        pullIndicatorView.setLayoutParams(params);

        invalidate();

        if (recyclerView.getAdapter() != null) {
            recyclerView.scrollToPosition(recyclerView.getAdapter().getItemCount() - 1);
        }

    }

    private void closeRefreshView() {
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) pullIndicatorView.getLayoutParams();
        if (params.height > 0) {
            ValueAnimator animation = ValueAnimator.ofFloat(params.height, 0);
            animation.setDuration(closeAnimationDuration);

            animation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator updatedAnimation) {
                    float animatedValue = (float) updatedAnimation.getAnimatedValue();
                    setRefreshableViewHeight(animatedValue);
                }
            });

            animation.start();
        }
    }

    private final View.OnTouchListener onTouchListener = new View.OnTouchListener() {
        private float startingPoint;


        @Override
        public boolean onTouch(View v, MotionEvent event) {

            if (pullIndicatorView == null || refreshing) {
                return false;
            }

            switch (event.getAction()) {
                case MotionEvent.ACTION_UP:
                    if (aboveThreshold) {
                        refreshing = true;
                        listener.onRefresh();
                    } else {
                        propagateTouchEvent = false;
                        setRefreshableViewHeight(0);
                    }

                    aboveThreshold = false;
                    break;
                case MotionEvent.ACTION_DOWN:
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (propagateTouchEvent) {
                        float difference = startingPoint - event.getY();
                        if (difference < 0) {
                            propagateTouchEvent = false;
                            setRefreshableViewHeight(0);
                        } else {
                            setRefreshableViewHeight(difference);
                        }
                    } else {
                        if (!recyclerView.canScrollVertically(1)) {
                            if (propagateTouchEvent) {

                                float difference = startingPoint - event.getY();
                                if (difference < 0) {
                                    propagateTouchEvent = false;
                                    setRefreshableViewHeight(0);
                                } else {
                                    setRefreshableViewHeight(difference);
                                }

                            } else {
                                propagateTouchEvent = true;
                                startingPoint = event.getY();
                            }
                        }
                    }
                    break;
            }

            return propagateTouchEvent;
        }
    };

    public interface PullToRefreshListener {
        void onAboveThreshold();

        void onBehindThreshold();

        void onRefresh();
    }

    private final PullToRefreshListener defaultListener = new PullToRefreshListener() {
        @Override
        public void onAboveThreshold() {

        }

        @Override
        public void onBehindThreshold() {

        }

        @Override
        public void onRefresh() {

        }
    };

}
