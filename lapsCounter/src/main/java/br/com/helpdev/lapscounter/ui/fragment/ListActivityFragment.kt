package br.com.helpdev.lapscounter.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import br.com.helpdev.lapscounter.databinding.FragmentListActivityBinding
import br.com.helpdev.lapscounter.injector.InjectorUtils
import br.com.helpdev.lapscounter.model.entity.ActivityEntity
import br.com.helpdev.lapscounter.ui.adapter.ListActivityAdapter
import br.com.helpdev.lapscounter.ui.viewmodel.ListActivityViewModel

class ListActivityFragment : Fragment() {

    private var _binding: FragmentListActivityBinding? = null
    private val binding get() = requireNotNull(_binding)
    private lateinit var listActivityAdapter: ListActivityAdapter
    private lateinit var viewModel: ListActivityViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentListActivityBinding.inflate(inflater, container, false)
        subscribeUI(binding)
        return binding.root
    }

    private fun subscribeUI(binding: FragmentListActivityBinding) {
        listActivityAdapter = createAdapter()
        binding.recyclerView.adapter = listActivityAdapter
        viewModel = loadViewModel()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.observerActivities(listActivityAdapter, binding)
    }

    private fun createAdapter() = ListActivityAdapter { navigateToActivity(it) }

    private fun loadViewModel() = ViewModelProvider(
        this,
        InjectorUtils.provideActivityViewModelFactory()
    )[ListActivityViewModel::class.java]

    private fun ListActivityViewModel.observerActivities(
        adapter: ListActivityAdapter,
        binding: FragmentListActivityBinding
    ) {
        activities.observe(viewLifecycleOwner, Observer { realmResults ->
            val toList = realmResults.toList()
            if (toList.isNullOrEmpty()) {
                binding.hasItems = false
            } else {
                binding.hasItems = true
                adapter.submitList(toList)
            }
        })
    }

    private fun navigateToActivity(activityEntity: ActivityEntity) {
        Navigation.findNavController(requireView()).navigate(
            ListActivityFragmentDirections.actionListActivityFragmentToActivityFragment(activityEntity.id)
        )
    }

    override fun onDestroyView() {
        binding.recyclerView.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
