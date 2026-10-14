package br.com.helpdev.lapscounter.ui.fragment

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import br.com.helpdev.lapscounter.R
import br.com.helpdev.lapscounter.databinding.FragmentActivityBinding
import br.com.helpdev.lapscounter.injector.InjectorUtils
import br.com.helpdev.lapscounter.model.entity.ActivityEntity
import br.com.helpdev.lapscounter.model.entity.toObLaps
import br.com.helpdev.lapscounter.ui.ActivitiesActivity
import br.com.helpdev.lapscounter.ui.adapter.LapsAdapter
import br.com.helpdev.lapscounter.ui.viewmodel.ActivityViewModel
import br.com.helpdev.lapscounter.utils.ChronometerShareUtils
import com.google.android.gms.ads.AdView
import com.google.android.material.snackbar.Snackbar
import br.com.helpdev.lapscounter.ui.ads.AdsManager


class ActivityFragment : Fragment() {

    private var dialog: AlertDialog? = null
    private lateinit var viewModel: ActivityViewModel
    private var _binding: FragmentActivityBinding? = null
    private val binding get() = requireNotNull(_binding)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onPause() {
        super.onPause()
        dismissDialog()
    }

    private fun dismissDialog() {
        dialog?.let {
            it.dismiss()
            dialog = null
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentActivityBinding.inflate(inflater, container, false)
        subscribeUI(binding)
        return binding.root
    }

    private fun subscribeUI(binding: FragmentActivityBinding) {
        val activityEntityId = ActivityFragmentArgs.fromBundle(requireArguments()).activityEntityId
        viewModel = loadViewModel()
        binding.viewModel = viewModel
        viewModel.init(requireContext(), activityEntityId)
    }

    private fun observerActivityEntity(
        viewModel: ActivityViewModel,
        binding: FragmentActivityBinding
    ) {
        viewModel.activityEntity.observe(viewLifecycleOwner, Observer { activityEntity ->
            configureLapsLog(activityEntity, binding)
            setToolbarTitle(activityEntity.name)
        })
    }

    private fun loadViewModel() = ViewModelProvider(
        this,
        InjectorUtils.provideActivityViewModelFactory()
    )[ActivityViewModel::class.java]

    private fun configureLapsLog(activityEntity: ActivityEntity, binding: FragmentActivityBinding) {
        val toObLaps = activityEntity.chronometer!!.toObLaps()
        binding.layoutLog.recyclerView.adapter = LapsAdapter(requireContext(), toObLaps, false)
        if (toObLaps.isNotEmpty()) binding.layoutLog.textViewEmpty.visibility = View.GONE
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observerActivityEntity(viewModel, binding)
        (requireActivity() as MenuHost).addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.activity_saved_menu, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean = when (menuItem.itemId) {
                    R.id.menu_remove -> { showDialogDelete(); true }
                    R.id.menu_share -> { shareActivity(); true }
                    else -> false
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )
        AdsManager.loadBanner(binding.root.findViewById<AdView>(R.id.adView))
    }

    private fun setToolbarTitle(name: String) {
        (activity as ActivitiesActivity).supportActionBar?.title = name
    }

    private fun showDialogDelete() {
        AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.app_name)
            setMessage(R.string.confirm_delete)
            setPositiveButton(android.R.string.ok) { _, _ -> deleteActivity() }
            setNegativeButton(android.R.string.cancel, null)
        }.let { dialog = it.create(); dialog!! }.let { it.show() }
    }

    private fun shareActivity() {
        viewModel.activityEntity.value?.let {
            ChronometerShareUtils.shareText(requireContext(), it)
        }
    }

    private fun deleteActivity() {
        viewModel.deleteActivity()
        Snackbar.make(binding.root, getString(R.string.activity_removed), Snackbar.LENGTH_LONG).show()
        Navigation.findNavController(binding.root).navigateUp()
    }


    override fun onDestroyView() {
        dismissDialog()
        _binding = null
        super.onDestroyView()
    }

}
