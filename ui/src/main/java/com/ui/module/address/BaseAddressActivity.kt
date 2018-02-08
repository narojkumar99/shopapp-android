package com.ui.module.address

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.TextWatcher
import android.view.View
import com.domain.entity.Address
import com.domain.entity.Country
import com.domain.entity.State
import com.ui.R
import com.ui.base.lce.BaseActivity
import com.ui.base.lce.view.LceLayout
import com.ui.base.picker.BaseBottomSheetPicker
import com.ui.custom.SimpleTextWatcher
import com.ui.ext.getTrimmedString
import com.ui.ext.hideKeyboard
import com.ui.module.address.contract.AddressPresenter
import com.ui.module.address.contract.AddressView
import kotlinx.android.synthetic.main.activity_address.*
import kotlinx.android.synthetic.main.activity_lce.*
import javax.inject.Inject

abstract class BaseAddressActivity<V : AddressView, P : AddressPresenter<V>> :
    BaseActivity<Address?, V, P>(),
    AddressView {

    companion object {
        private const val ADDRESS = "address"
    }

    @Inject
    lateinit var addressPresenter: P
    protected var isEditMode = false
    private var address: Address? = null
    private lateinit var fieldTextWatcher: TextWatcher
    private lateinit var countryPicker: CountryBottomSheetPicker
    private lateinit var statePicker: StateBottomSheetPicker

    //ANDROID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupMode()
        initTextWatcher()
        setupListeners()
        loadCountries()
    }


    override fun onResume() {
        super.onResume()
        if (this::fieldTextWatcher.isInitialized) {
            countryInput.addTextChangedListener(fieldTextWatcher)
            firstNameInput.addTextChangedListener(fieldTextWatcher)
            lastNameInput.addTextChangedListener(fieldTextWatcher)
            addressInput.addTextChangedListener(fieldTextWatcher)
            cityInput.addTextChangedListener(fieldTextWatcher)
            postalCodeInput.addTextChangedListener(fieldTextWatcher)
            phoneInput.addTextChangedListener(fieldTextWatcher)
            stateInput.addTextChangedListener(fieldTextWatcher)
        }
    }

    override fun onPause() {
        super.onPause()
        countryInput.removeTextChangedListener(fieldTextWatcher)
        firstNameInput.removeTextChangedListener(fieldTextWatcher)
        lastNameInput.removeTextChangedListener(fieldTextWatcher)
        addressInput.removeTextChangedListener(fieldTextWatcher)
        cityInput.removeTextChangedListener(fieldTextWatcher)
        postalCodeInput.removeTextChangedListener(fieldTextWatcher)
        phoneInput.removeTextChangedListener(fieldTextWatcher)
        stateInput.removeTextChangedListener(fieldTextWatcher)
    }

    //INIT

    override fun getContentView() = R.layout.activity_address

    override fun createPresenter() = addressPresenter

    private fun initTextWatcher() {
        fieldTextWatcher = object : SimpleTextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                checkInputFields()
            }
        }
    }

    //SETUP

    private fun setupMode() {
        address = intent.getParcelableExtra(ADDRESS)
        isEditMode = address != null

        val titleRes: Int?

        if (isEditMode) {
            titleRes = R.string.edit_address
            submitButton.setText(R.string.edit)
            stateInputContainer.visibility = View.VISIBLE
            fillFields(address)
            checkInputFields()
        } else {
            submitButton.setText(R.string.submit)
            titleRes = R.string.add_new_address
        }

        setTitle(getString(titleRes))
    }

    private fun setupListeners() {
        submitButton.setOnClickListener {
            changeState(LceLayout.LceState.LoadingState(true))
            submitButton.requestFocus()
            submitButton.hideKeyboard()
            submitAddress()
        }

        countryInput.setOnClickListener {
            it.hideKeyboard()
            countryPicker.show(supportFragmentManager, "", countryInput.text.toString())
        }

        stateInput.setOnClickListener {
            it.hideKeyboard()
            statePicker.show(supportFragmentManager, "", stateInput.text.toString())
        }
    }

    protected open fun submitAddress() {
        if (isEditMode) {
            address?.let {
                presenter.editAddress(
                    it.id,
                    getAddress()
                )
            }
        } else {
            presenter.submitAddress(getAddress())
        }
    }

    private fun checkInputFields() {
        var isEnabled = countryInput.text.isNotBlank() &&
                firstNameInput.text.isNotBlank() &&
                lastNameInput.text.isNotBlank() &&
                addressInput.text.isNotBlank() &&
                cityInput.text.isNotBlank() &&
                postalCodeInput.text.isNotBlank() &&
                phoneInput.text.isNotBlank()

        if (stateInputContainer.visibility == View.VISIBLE) {
            isEnabled = isEnabled && stateInput.text.isNotBlank()
        }

        submitButton.isEnabled = isEnabled
    }

    protected fun getAddress() = Address(
        address = addressInput.getTrimmedString(),
        secondAddress = secondAddressInput.getTrimmedString(),
        city = cityInput.getTrimmedString(),
        country = countryInput.getTrimmedString(),
        state = stateInput.getTrimmedString(),
        firstName = firstNameInput.getTrimmedString(),
        lastName = lastNameInput.getTrimmedString(),
        zip = postalCodeInput.getTrimmedString().toUpperCase(),
        phone = phoneInput.getTrimmedString()
    )

    private fun fillFields(address: Address?) {
        address?.let {
            firstNameInput.setText(it.firstName)
            lastNameInput.setText(it.lastName)
            addressInput.setText(it.address)
            secondAddressInput.setText(it.secondAddress)
            cityInput.setText(it.city)
            stateInput.setText(it.state ?: "")
            countryInput.setText(it.country)
            postalCodeInput.setText(it.zip)
            phoneInput.setText(it.phone ?: "")
        }
    }

    private fun setupCountries(countries: List<Country>) {
        countryPicker = CountryBottomSheetPicker.newInstance()
        countryPicker.setData(countries)
        countryPicker.onDoneButtonClickedListener = object : BaseBottomSheetPicker.OnDoneButtonClickedListener<Country> {
            override fun onDoneButtonClicked(selectedData: Country) {
                countryInput.setText(selectedData.name)
                setupStates(selectedData)
            }
        }
    }

    private fun setupStates(country: Country) {
        if (country.states != null && country.states!!.isEmpty()) {
            stateInputContainer.visibility = View.GONE
        } else {
            stateInputContainer.visibility = View.VISIBLE
            statePicker = StateBottomSheetPicker.newInstance()
            statePicker.setData(country.states!!)
            statePicker.onDoneButtonClickedListener = object : BaseBottomSheetPicker.OnDoneButtonClickedListener<State> {
                override fun onDoneButtonClicked(selectedData: State) {
                    stateInput.setText(selectedData.name)
                }
            }
        }
    }

    //LCE

    override fun countriesLoaded(countries: List<Country>) {
        lceLayout.changeState(LceLayout.LceState.ContentState)

        setupCountries(countries)
        address?.let {
            val index = countries.map { it.name }.indexOf(it.country)
            if (index >= 0) {
                setupStates(countries[index])
            }
        }

    }

    override fun showContent(data: Address?) {
        super.showContent(data)
        fillFields(data)
    }

    override fun addressChanged(address: Address) {
        val result = Intent()
        setResult(Activity.RESULT_OK, result)
        finish()
    }

    override fun submitAddressError() {
        changeState(LceLayout.LceState.ContentState)
    }

    private fun loadCountries() {
        lceLayout.changeState(LceLayout.LceState.LoadingState(true))
        presenter.getCountriesList()
    }

}