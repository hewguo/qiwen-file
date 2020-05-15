package com.mac.scp.controller;

import com.alibaba.fastjson.JSON;
import com.mac.common.cbb.DateUtil;
import com.mac.common.cbb.RestResult;
import com.mac.common.operation.FileOperation;
import com.mac.common.operation.ImageOperation;
import com.mac.common.util.FileUtil;
import com.mac.common.util.PathUtil;
import com.mac.scp.api.IFileService;
import com.mac.scp.domain.FileBean;
import com.mac.scp.domain.TreeNode;
import com.mac.scp.domain.UserBean;
import org.apache.shiro.SecurityUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.mac.common.util.FileUtil.getFileExtendsByType;

@RestController
@RequestMapping("/file")
public class FileController {

	/**
	 * 是否开启共享文件模式
	 */
	public static Boolean isShareFile = false;
	public static long treeid = 0;
	@Resource
	IFileService fileService;


	/**
	 * 创建文件
	 *
	 * @return
	 */
	// TODO 创建文件
	@PostMapping("/createfile")
	@ResponseBody
	public RestResult<String> createFile(@RequestBody FileBean fileBean) {
		RestResult<String> restResult = new RestResult<>();
		if (!operationCheck().isSuccess()) {
			return operationCheck();
		}

		UserBean sessionUserBean = (UserBean) SecurityUtils.getSubject().getPrincipal();
		fileBean.setUserid(sessionUserBean.getUserId());

		fileBean.setUploadtime(DateUtil.getCurrentTime());

		fileService.insertFile(fileBean);
		restResult.setSuccess(true);
		return restResult;
	}

	// TODO 获取文件列表
	@GetMapping("/getfilelist")
	@ResponseBody
	public RestResult<List<FileBean>> getFileList(FileBean fileBean) {
		RestResult<List<FileBean>> restResult = new RestResult<>();
		if (isShareFile) {
			fileBean.setUserid(2);
		} else {
			UserBean sessionUserBean = (UserBean) SecurityUtils.getSubject().getPrincipal();
			if (fileBean == null) {
				restResult.setSuccess(false);
				return restResult;
			}
			fileBean.setUserid(sessionUserBean.getUserId());
		}

		fileBean.setFilepath(PathUtil.urlDecode(fileBean.getFilepath()));
		List<FileBean> fileList = fileService.selectFileList(fileBean);


		restResult.setData(fileList);
		restResult.setSuccess(true);
		return restResult;
	}

	/**
	 * 批量删除文件
	 *
	 * @return
	 */
	// TODO 批量删除文件
	@PostMapping("/batchdeletefile")
	@ResponseBody
	public RestResult<String> deleteImageByIds(@RequestBody FileBean fileBean) {
		RestResult<String> result = new RestResult<String>();
		if (!operationCheck().isSuccess()) {
			return operationCheck();
		}


		String files = fileBean.getFiles();
		List<FileBean> fileList = JSON.parseArray(files, FileBean.class);

		for (FileBean file : fileList) {
			fileService.deleteFile(file);
		}

		result.setData("批量删除文件成功");
		result.setSuccess(true);
		return result;
	}

	/**
	 * 删除文件
	 *
	 * @return
	 */
	// TODO 删除文件
	@PostMapping("/deletefile")
	@ResponseBody
	public RestResult<String> deleteFile(@RequestBody FileBean fileBean) {
		RestResult<String> result = new RestResult<String>();
		if (!operationCheck().isSuccess()) {
			return operationCheck();
		}
		fileService.deleteFile(fileBean);
		result.setSuccess(true);
		return result;
	}

	/**
	 * 解压文件
	 *
	 * @return
	 */
	// TODO 解压文件
	@PostMapping("/unzipfile")
	@ResponseBody
	public RestResult<String> unzipFile(@RequestBody FileBean fileBean) {
		RestResult<String> result = new RestResult<String>();
		if (!operationCheck().isSuccess()) {
			return operationCheck();
		}

		String zipFileUrl = PathUtil.getStaticPath() + fileBean.getFileurl();
		File file = FileOperation.newFile(zipFileUrl);
		String unzipUrl = file.getParent();

		List<String> fileEntryNameList = FileOperation.unzip(file, unzipUrl);

		List<FileBean> fileBeanList = new ArrayList<>();
		UserBean sessionUserBean = (UserBean) SecurityUtils.getSubject().getPrincipal();
		for (String entryName : fileEntryNameList) {
			String totalFileUrl = unzipUrl + entryName;
			File currentFile = FileOperation.newFile(totalFileUrl);

			FileBean tempFileBean = new FileBean();
			tempFileBean.setUploadtime(DateUtil.getCurrentTime());
			tempFileBean.setUserid(sessionUserBean.getUserId());
			tempFileBean.setFilepath(FileUtil.pathSplitFormat(fileBean.getFilepath() + entryName.replace(currentFile.getName(), "")));
			if (currentFile.isDirectory()) {

				tempFileBean.setIsdir(1);

				tempFileBean.setFilename(currentFile.getName());
				tempFileBean.setTimestampname(currentFile.getName());
				//tempFileBean.setFileurl(File.separator + (file.getParent() + File.separator + currentFile.getName()).replace(PathUtil.getStaticPath(), ""));
			} else {

				tempFileBean.setIsdir(0);
				String fileType = FileUtil.getFileType(totalFileUrl);
				tempFileBean.setExtendname(fileType);
				tempFileBean.setFilename(FileUtil.getFileNameNotExtend(currentFile.getName()));
				tempFileBean.setFilesize(currentFile.length());
				tempFileBean.setTimestampname(FileUtil.getFileNameNotExtend(currentFile.getName()));
				tempFileBean.setFileurl(File.separator + (currentFile.getPath()).replace(PathUtil.getStaticPath(), ""));
				if (FileUtil.isImageFile(fileType)){
					String minFileUrl = totalFileUrl.replace("." + fileType, "_min." + fileType);
					File minFile = FileOperation.newFile(minFileUrl);
					try {
						ImageOperation.thumbnailsImage(currentFile, minFile, 300);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			fileBeanList.add(tempFileBean);
		}
		fileService.batchInsertFile(fileBeanList);
		result.setSuccess(true);
		return result;
	}

	/**
	 * 文件移动
	 *
	 * @return 返回前台移动结果
	 */
	// TODO  移动文件
	@PostMapping("/movefile")
	@ResponseBody
	public RestResult<String> moveFile(@RequestBody FileBean fileBean) {
		RestResult<String> result = new RestResult<String>();
		if (!operationCheck().isSuccess()) {
			return operationCheck();
		}
		String oldfilepath = fileBean.getOldfilepath();
		String newfilepath = fileBean.getNewfilepath();
		String filename = fileBean.getFilename();
		String extendname = fileBean.getExtendname();

		fileService.updateFilepathByFilepath(oldfilepath, newfilepath, filename, extendname);
		result.setSuccess(true);
		return result;
	}

	/**
	 * 批量移动文件
	 *
	 * @return 返回前台移动结果
	 */
	// TODO 批量移动文件
	@PostMapping("/batchmovefile")
	@ResponseBody
	public RestResult<String> batchMoveFile(@RequestBody FileBean fileBean) {
		RestResult<String> result = new RestResult<String>();
		if (!operationCheck().isSuccess()) {
			return operationCheck();
		}
		String files = fileBean.getFiles();
		String newfilepath = fileBean.getNewfilepath();

		List<FileBean> fileList = JSON.parseArray(files, FileBean.class);

		for (FileBean file : fileList) {
			fileService.updateFilepathByFilepath(file.getFilepath(), newfilepath, file.getFilename(), file.getExtendname());
		}

		result.setData("批量移动文件成功");
		result.setSuccess(true);
		return result;
	}

	public RestResult<String> operationCheck() {
		RestResult<String> result = new RestResult<String>();
		UserBean sessionUserBean = (UserBean) SecurityUtils.getSubject().getPrincipal();
		if (sessionUserBean == null) {
			result.setSuccess(false);
			result.setErrorMessage("未登录");
			return result;
		}
		if (isShareFile) {
			if (sessionUserBean.getUserId() > 2) {
				result.setSuccess(false);
				result.setErrorMessage("没权限，请联系管理员！");
				return result;
			}
		}
		result.setSuccess(true);
		return result;
	}

	/**
	 * 通过文件类型选择文件
	 *
	 * @param fileBean 文件类型
	 * @return
	 */
	// TODO 通过文件类型选择文件
	@GetMapping("/selectfilebyfiletype")
	@ResponseBody
	public RestResult<List<FileBean>> selectFileByFileType(FileBean fileBean) {
		RestResult<List<FileBean>> result = new RestResult<List<FileBean>>();
		UserBean sessionUserBean = (UserBean) SecurityUtils.getSubject().getPrincipal();
		long userid = sessionUserBean.getUserId();
		if (isShareFile) {
			userid = 2;
		}
		List<FileBean> file = fileService.selectFileByExtendName(getFileExtendsByType(fileBean.getFiletype()), userid);
		result.setData(file);
		result.setSuccess(true);
		return result;
	}

	/**
	 * 获取文件树
	 *
	 * @return
	 */
	// TODO 获取文件的树结构
	@GetMapping("/getfiletree")
	@ResponseBody
	public RestResult<TreeNode> getFileTree() {
		RestResult<TreeNode> result = new RestResult<TreeNode>();
		FileBean fileBean = new FileBean();
		UserBean sessionUserBean = (UserBean) SecurityUtils.getSubject().getPrincipal();
		if (isShareFile) {
			fileBean.setUserid(2);
		} else {
			fileBean.setUserid(sessionUserBean.getUserId());
		}

		List<FileBean> filePathList = fileService.selectFilePathTreeByUserid(fileBean);
		TreeNode resultTreeNode = new TreeNode();
		resultTreeNode.setNodeName("/");

		for (FileBean bean : filePathList) {
			String filePath = bean.getFilepath() + bean.getFilename() + "/";

			Queue<String> queue = new LinkedList<>();

			String[] strArr = filePath.split("/");
			for (String s : strArr) {
				if (!"".equals(s) && s != null) {
					queue.add(s);
				}

			}
			if (queue.size() == 0) {
				continue;
			}
			resultTreeNode = insertTreeNode(resultTreeNode, "/", queue);


		}
		result.setSuccess(true);
		result.setData(resultTreeNode);
		return result;

	}

	public TreeNode insertTreeNode(TreeNode treeNode, String filepath, Queue<String> nodeNameQueue) {

		List<TreeNode> childrenTreeNodes = treeNode.getChildren();
		String currentNodeName = nodeNameQueue.peek();
		if (currentNodeName == null) {
			return treeNode;
		}

		Map<String, String> map = new HashMap<>();
		filepath = filepath + currentNodeName + "/";
		map.put("filepath", filepath);
		//1、判断有没有该子节点，如果没有则插入
		if (!isExistPath(childrenTreeNodes, currentNodeName)) {
			//插入
			TreeNode resultTreeNode = new TreeNode();


			resultTreeNode.setAttributes(map);
			resultTreeNode.setNodeName(nodeNameQueue.poll());
			resultTreeNode.setId(treeid++);

			childrenTreeNodes.add(resultTreeNode);

		} else {  //2、如果有，则跳过
			nodeNameQueue.poll();
		}

		if (nodeNameQueue.size() != 0) {
			for (int i = 0; i < childrenTreeNodes.size(); i++) {

				TreeNode childrenTreeNode = childrenTreeNodes.get(i);
				if (currentNodeName.equals(childrenTreeNode.getLabel())) {
					childrenTreeNode = insertTreeNode(childrenTreeNode, filepath, nodeNameQueue);
					childrenTreeNodes.remove(i);
					childrenTreeNodes.add(childrenTreeNode);
					treeNode.setChildNode(childrenTreeNodes);
				}

			}
		} else {
			treeNode.setChildNode(childrenTreeNodes);
		}

		return treeNode;

	}

	public boolean isExistPath(List<TreeNode> childrenTreeNodes, String path) {
		boolean isExistPath = false;

		try {
			for (TreeNode childrenTreeNode : childrenTreeNodes) {
				if (path.equals(childrenTreeNode.getLabel())) {
					isExistPath = true;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}


		return isExistPath;
	}


}
