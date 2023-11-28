package org.briarproject.briar.android.account;

import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.briarproject.briar.R;
import org.briarproject.briar.android.login.StrengthMeter;
import org.briarproject.briar.android.util.AccountSetUpCriteria;
import org.briarproject.nullsafety.MethodsNotNullByDefault;
import org.briarproject.nullsafety.ParametersNotNullByDefault;

import java.util.ArrayList;
import java.util.Arrays;

import javax.annotation.Nullable;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission;

import static android.Manifest.permission.POST_NOTIFICATIONS;
import static android.content.Context.INPUT_METHOD_SERVICE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.os.Build.VERSION.SDK_INT;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static android.view.inputmethod.EditorInfo.IME_ACTION_DONE;
import static androidx.core.content.ContextCompat.checkSelfPermission;
import static org.briarproject.bramble.api.crypto.PasswordStrengthEstimator.QUITE_WEAK;
import static org.briarproject.briar.android.util.UiUtils.setError;

@MethodsNotNullByDefault
@ParametersNotNullByDefault
public class SetPasswordFragment extends SetupFragment {
	private final static String TAG = SetPasswordFragment.class.getName();

	private TextInputLayout passwordEntryWrapper;
	private TextInputLayout passwordConfirmationWrapper;
	private TextInputEditText passwordEntry;
	private TextInputEditText passwordConfirmation;
	private TextView passwordCriteria;
	private StrengthMeter strengthMeter;
	private Button nextButton;

	private final ActivityResultLauncher<String> requestPermissionLauncher =
			registerForActivityResult(new RequestPermission(), isGranted ->
					setPassword());

	public static SetPasswordFragment newInstance() {
		return new SetPasswordFragment();
	}

	@Override
	public View onCreateView(LayoutInflater inflater,
			@Nullable ViewGroup container,
			@Nullable Bundle savedInstanceState) {
		requireActivity().setTitle(getString(R.string.setup_password_intro));
		View v = inflater.inflate(R.layout.fragment_setup_password, container,
				false);

		strengthMeter = v.findViewById(R.id.strength_meter);
		passwordEntryWrapper = v.findViewById(R.id.password_entry_wrapper);
		passwordEntry = v.findViewById(R.id.password_entry);
		passwordConfirmationWrapper =
				v.findViewById(R.id.password_confirm_wrapper);
		passwordConfirmation = v.findViewById(R.id.password_confirm);
		nextButton = v.findViewById(R.id.next);
		passwordCriteria = v.findViewById(R.id.password_criteria);
		ProgressBar progressBar = v.findViewById(R.id.progress);

		passwordEntry.addTextChangedListener(this);
		passwordConfirmation.addTextChangedListener(this);
		nextButton.setOnClickListener(this);

		if (!viewModel.needToShowDozeFragment()) {
			nextButton.setText(R.string.create_account_button);
			int options = passwordConfirmation.getImeOptions();
			passwordConfirmation.setImeOptions(options | IME_ACTION_DONE);
		}

		viewModel.getIsCreatingAccount()
				.observe(getViewLifecycleOwner(), isCreatingAccount -> {
					if (isCreatingAccount) {
						nextButton.setVisibility(INVISIBLE);
						progressBar.setVisibility(VISIBLE);
						// this also avoids the keyboard popping up
						passwordEntry.setFocusable(false);
						passwordConfirmation.setFocusable(false);
					}
				});

		return v;
	}

	@Override
	public String getUniqueTag() {
		return TAG;
	}

	@Override
	protected String getHelpText() {
		return getString(R.string.setup_password_explanation);
	}

	@Override
	public void onTextChanged(CharSequence authorName, int i, int i1, int i2) {
		String password1 = passwordEntry.getText().toString();
		String password2 = passwordConfirmation.getText().toString();
		boolean passwordsMatch = password1.equals(password2);

		strengthMeter
				.setVisibility(password1.length() > 0 ? VISIBLE : INVISIBLE);
		float strength = viewModel.estimatePasswordStrength(password1);
		strengthMeter.setStrength(strength);
		boolean strongEnough = strength >= QUITE_WEAK;

		setError(passwordEntryWrapper, getString(R.string.password_too_weak),
				password1.length() > 0 && !strongEnough);
		setError(passwordConfirmationWrapper,
				getString(R.string.passwords_do_not_match),
				password2.length() > 0 && !passwordsMatch);

		AccountSetUpCriteria accountSetUpCriteria = new AccountSetUpCriteria();
		boolean meetsCriteria = accountSetUpCriteria.checkCriteria(password1);
		if (!meetsCriteria) {
			passwordCriteria.setVisibility(VISIBLE);
		} else {
			passwordCriteria.setVisibility(INVISIBLE);
		}

		boolean enabled = passwordsMatch && strongEnough && meetsCriteria;
		nextButton.setEnabled(enabled);
		passwordConfirmation.setOnEditorActionListener(enabled ? this : null);

	}

	@Override
	public void onClick(View view) {
		IBinder token = passwordEntry.getWindowToken();
		Object o = requireContext().getSystemService(INPUT_METHOD_SERVICE);
		((InputMethodManager) o).hideSoftInputFromWindow(token, 0);
		if (SDK_INT >= 33 &&
				checkSelfPermission(requireContext(), POST_NOTIFICATIONS) !=
						PERMISSION_GRANTED) {
			// this calls setPassword() when it returns
			requestPermissionLauncher.launch(POST_NOTIFICATIONS);
		} else {
			setPassword();
		}
	}

	@Override
	public void setPassword() {
		viewModel.setPassword(passwordEntry.getText().toString());
	}

}