import React, {Component} from 'react';
import { withRouter } from 'react-router-dom';
import './Common.css';
import './SiteTable.css';
import MessagesLoader from "./MessagesLoader";
import cx from 'classnames';

class SiteTable extends Component {
  constructor(props) {
    super(props);
    this.state = {
      deleteErrorMessage: '',
      messages: MessagesLoader.Empty
    };
  }
  
  componentDidMount = async() => {
    try {
      this.setState({
        messages: await new MessagesLoader().load([
          { key: 'recordEmpty'},
          { key: 'siteName'},
          { key: 'dateTime'},
          { key: 'timeZone'},
          { key: 'administrator'},
          { key: 'deleteConfirm'},
          { key: 'agentRecordWillBeDeleted'},
          { key: 'delete'},
          { key: 'cancel'},
          { key: 'deleteSite'}
        ])
      });
    } catch (e) {
      console.log("Error: " + JSON.stringify(e));
    }
  }

  msg = (key) => {
    return this.state.messages(key);
  }

  showDeleteDialog = (siteId, siteName) => {
    this.setState({
      deleteCandidate: {
        siteId: siteId,
        siteName: siteName
      }
    });
  }

  cancelDelete = (e) => {
    this.setState({
      deleteCandidate: undefined
    });
  }

  deleteSite = async(siteId) => {
    try {
      const resp = await fetch(
        "/api/deleteSite?siteId=" + siteId, {
          method: "POST",
          headers: {
            "Csrf-Token": "nocheck"
          },
          body: ''
        }
      );

      if (resp.status === 200) {
        this.setState({
          deleteCandidate: undefined
        });
        this.props.onSiteDeleted(siteId);
      } else if (resp.status === 403) {
        this.setState({
          deleteErrorMessage: this.msg('unknownError')
        });
      }
    } catch (e) {
      console.log("error: " + JSON.stringify(e));
    }
  }

  onSiteClicked = (siteId) => {
    this.props.history.push("/attend/" + siteId);
  }

  selectSite = (siteId, siteName, datetime, timezone) => {
    const dates = datetime.split(' ');
    const dateElems = dates[0].split('/');
    const date = new Date(dateElems[0], dateElems[1] - 1, dateElems[2]);
    const time = dates[1];

    if (this.props.onSiteSelected)
      this.props.onSiteSelected(siteId, siteName, date, time, timezone);
    this.setState({
      selectedSiteId: siteId
    });
  }

  showAgentRecords = (siteId) => {
    this.props.history.push("/agentRecords/" + siteId);
  }

  render() {
    const table = this.props.records.length === 0 ?
          <span className="emptyMessage">{this.msg('recordEmpty')}</span>
          :
          <table className="table is-striped sites">
            <thead>
              <tr>
                <th className='siteName'>{this.msg('siteName')}</th>
                <th className='dateTime'>{this.msg('dateTime')}</th>
                <th className='timeZone'>{this.msg('timeZone')}</th>
                <th className='administrator'>{this.msg('administrator')}</th>
                <th className='function'></th>
              </tr>
            </thead>
            <tbody id="siteTable">
              {
                this.props.records.map((e) =>
                  <tr key={e.siteId}
                      className={this.state.selectedSiteId !== undefined && this.state.selectedSiteId === e.siteId ? 'selected' : ''}>
                    <td className='siteName'>
                      <span className="siteNameBody">{e.siteName}</span>
                      &nbsp;
                      <a href="#attend" className="is-info button attend"
                         onClick={(ev) => this.onSiteClicked(e.siteId)}>
                        <i className="fas fa-external-link-alt"></i>
                      </a>
                    </td>
                    <td className="dateTime">{e.dateTime}</td>
                    <td className="timeZone">{e.timeZone}</td>
                    <td className="administrator">{e.owner}</td>
                    <td className="function">
                      { this.props.canDeleteSite ? (
                        <button className="is-info button" title={this.msg("edit")}
                                onClick={(ev) => this.selectSite(e.siteId, e.siteName, e.dateTime, e.timeZone)}>
                          <i className="fas fa-pencil-alt"></i>
                          </button>)  : ""}
                      &nbsp;
                      { this.props.canDeleteSite ? (
                        <button className="is-danger button" title={this.msg("deleteSite")}
                           onClick={ev => this.showDeleteDialog(e.siteId, e.siteName)}>
                          <i className="far fa-trash-alt"></i>
                          </button>)  : ""}
                      &nbsp;
                      { this.props.canDeleteSite ? (
                        <button className="is-info button agentRecords" title={this.msg("agentRecords")}
                           onClick={ev => this.showAgentRecords(e.siteId)}>
                          <i className="fas fa-poll-h"></i>&nbsp;
                        </button>)  : ""}
                    </td>
                  </tr>
                )
              }
            </tbody>
          </table>;

    return (
      <div>
        {table}
        
        { /* Modals */ }
        <div className={cx("modal", {'is-active': this.state.deleteCandidate !== undefined})}>
          <div className="modal-background"></div>
          <div className="modal-content">
            <div className='dialogSiteName'>
              { this.state.deleteCandidate !== undefined ? this.state.deleteCandidate.siteName : "" }
            </div>
            <div>
              { this.msg('deleteConfirm') }
            </div>
            <div>
              { this.msg('agentRecordWillBeDeleted') }
            </div>
            <div className='dialogButtons'>
              <a href="#deleteSite" className="button is-danger"
                 onClick={(e) => {this.deleteSite(this.state.deleteCandidate.siteId);}}>
                {this.msg('delete')}
              </a>&nbsp;
              <a href="#deleteSite" className="button" onClick={(e) => {this.cancelDelete(e);}}>
                {this.msg('cancel')}
              </a>
            </div>
            <div className={cx("notification is-danger errorMessage", {'is-active': this.state.deleteErrorMessage !== ''})}>
              {this.state.deleteErrorMessage}
            </div>
          </div>
          <button className="modal-close is-large" aria-label="close" onClick={(e) => {this.cancelDelete(e);}}></button>
        </div>
      </div>
    );
  }
}

export default withRouter(SiteTable);
