package uet.app.mysound.ui.fragment.other

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import uet.app.mysound.adapter.moodandgenre.genre.GenreItemAdapter
import uet.app.mysound.data.model.explore.mood.genre.ItemsPlaylist
import uet.app.mysound.databinding.MoodMomentDialogBinding
import uet.app.mysound.utils.Resource
import uet.app.mysound.viewModel.GenreViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GenreFragment: Fragment() {
    private val viewModel by viewModels<GenreViewModel>()
    private var _binding: MoodMomentDialogBinding? = null
    private val binding get() = _binding!!

    private lateinit var genreList: ArrayList<ItemsPlaylist>
    private lateinit var genreItemAdapter: GenreItemAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = MoodMomentDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.contentLayout.visibility = View.GONE
        binding.loadingLayout.visibility = View.VISIBLE
        genreList = ArrayList()
        genreItemAdapter = GenreItemAdapter(arrayListOf(), requireContext(), findNavController())
        val linearLayoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.rvListPlaylist.apply {
            adapter = genreItemAdapter
            layoutManager = linearLayoutManager
        }
        val params = requireArguments().getString("params")
        if (viewModel.genreObject.value != null) {
            binding.contentLayout.visibility = View.VISIBLE
            binding.loadingLayout.visibility = View.GONE
            genreList.addAll(viewModel.genreObject.value?.data?.itemsPlaylist as ArrayList<ItemsPlaylist>)
            genreItemAdapter.updateData(genreList)
        } else {
            fetchData(params)
        }
        binding.topAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun fetchData(params: String?) {
        if (params != null) {
            viewModel.getGenre(params)
        }
        viewModel.genreObject.observe(viewLifecycleOwner, Observer { response ->
            when (response) {
                is Resource.Success -> {
                    response.data.let {
                        binding.topAppBar.title = it?.header
                        genreList.addAll(it?.itemsPlaylist as ArrayList<ItemsPlaylist>)
                        genreItemAdapter.updateData(genreList)
                        binding.contentLayout.visibility = View.VISIBLE
                        binding.loadingLayout.visibility = View.GONE
                    }
                }

                is Resource.Error -> {
                    binding.contentLayout.visibility = View.VISIBLE
                    binding.loadingLayout.visibility = View.GONE
                    response.message?.let { message ->
                        Snackbar.make(
                            binding.root,
                            "An error occured: $message",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }

                is Resource.Loading -> {
                    binding.contentLayout.visibility = View.GONE
                    binding.loadingLayout.visibility = View.VISIBLE
                }
            }
        })
    }
}